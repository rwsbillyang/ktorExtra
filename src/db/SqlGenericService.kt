/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-27 19:41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.ktorKit.db

import com.github.rwsbillyang.ktorKit.apiBox.UmiPagination
import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.AssignmentDeclaration
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.query.singleOrNull
import org.komapper.jdbc.JdbcDatabase

/**
 * basic CRUD service based on Komapper（https://github.com/komapper/komapper）
 *
 * <code>
 *     class CrudService(cache: ICache): SqlGenericService(cache) {
 *      private val dbSource: SqlDataSource by inject(qualifier = named(goodsModule.dbName!!))
 *      override val db: JdbcDatabase
 *        get() = dbSource.db
 *
 *        fun insertBrand(e: GoodsBrand) = insertOne(Meta.goodsBrand, e)
 *        fun updateBrand(e: GoodsBrand) = updateOneAndEvict("brand/${e.id}", Meta.goodsBrand, e)
 *        fun findBrand(id: Long) = findOneCacheable("brand/$id", Meta.goodsBrand){ Meta.goodsBrand.id eq id }
 *        fun deleteBrand(id: Long) = deleteAndEvictOne("brand/$id", Meta.goodsBrand){ Meta.goodsBrand.id eq id }
 *        }
 * </code>
 * */
abstract class SqlGenericService(cache: ICache) : CacheService(cache) {
    protected abstract val db: JdbcDatabase
    /**
     * find a record，evict cache if cacheKey or cacheKeys is not null
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param cacheKey cache key，evict if not null
     * @param database if null, use overridden db in subclass
     * @param w WhereDeclaration
     * @return a record or null
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> findOne(
        meta: META,
        cacheKey: String? = null,
        database: JdbcDatabase = db,
        w: WhereDeclaration
    ) =
        database.runQuery {
            QueryDsl.from(meta).where(w).singleOrNull()
        }?.also { if (cacheKey != null) cache.put(cacheKey, it) }

    /**
     * find all by WhereDeclaration
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param database if null, use overridden db in subclass
     * @param w WhereDeclaration
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> findAll(
        meta: META,
        database: JdbcDatabase = db,
        w: WhereDeclaration
    ) =
        database.runQuery {
            QueryDsl.from(meta).where(w)
        }


    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> findPage(
        meta: META,
        sortExpression: SortExpression,
        pagination: UmiPagination,
        database: JdbcDatabase = db,
        w: WhereDeclaration) = database.runQuery{
        if(pagination.lastId == null)
            QueryDsl.from(meta).where(w).orderBy(sortExpression).offset((pagination.current - 1) * pagination.pageSize).limit(pagination.pageSize)
        else{
            QueryDsl.from(meta).where(w).orderBy(sortExpression).limit(pagination.pageSize)
        }

    }

    /**
     * save(insert or update depends on isInsert) a record，use cache if cacheKey is not null
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param e entity data
     * @param isInsert insert if true, eg: id == null
     * @param cacheKey cache key，evict if not null
     * @param  updateCache if true, update cache, else evict cache，default false
     * @param database if null, use overridden db in subclass
     * @return the inserted/updated record
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> save(
        meta: META,
        e: ENTITY,
        isInsert: Boolean,
        cacheKey: String? = null,
        updateCache: Boolean = false,
        database: JdbcDatabase = db
    ) = database.runQuery {
        if(isInsert)
            QueryDsl.insert(meta).single(e)
        else
            QueryDsl.update(meta).single(e)
    }.also {
        if (cacheKey != null){
            if(isInsert){
                cache.put(cacheKey, it)
            }else{
                if (updateCache) cache.put(cacheKey, it) else cache.evict(cacheKey)
            }
        }
    }

    /**
     * save(insert or update depends on isInsert) a record，use cache if cacheKey is not null
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param list entity data list
     * @param isInsert insert if true, eg: id == null
     * @param database if null, use overridden db in subclass
     * @return the inserted/updated record
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> batchSave(
        meta: META,
        list: List<ENTITY>,
        isInsert: Boolean,
        database: JdbcDatabase = db
    ) = database.runQuery {
        if(isInsert)
            QueryDsl.insert(meta).multiple(list)
        else
            QueryDsl.update(meta).batch(list)
    }

    /**
     * update values of some fields，evict cache if cacheKey or cacheKeys is not null
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param setValuesDelDeclaration set value: eg: {Meta.goods.status eq EnumStatus.Deleted}
     * @param cacheKey cache key，evict if not null
     * @param cacheKeys cache keys，evict if not null
     * @param database if null, use overridden db in subclass
     * @param w WhereDeclaration
     * @return affected rows count
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> updateValues(
        meta: META,
        setValuesDelDeclaration: AssignmentDeclaration<ENTITY, META>,
        cacheKey: String? = null,
        cacheKeys: List<String>? = null,
        database: JdbcDatabase = db,
        w: WhereDeclaration
    ) =
        database.runQuery {
            QueryDsl.update(meta).set(setValuesDelDeclaration).where(w)
        }.also {
            if (cacheKey != null) cache.evict(cacheKey) else if (!cacheKeys.isNullOrEmpty()) cacheKeys.forEach {
                cache.evict(it)
            }
        }

    /**
     * delete record(s)，evict cache if cacheKey or cacheKeys is not null
     * @param meta Meta，refer to Komapper @KomapperEntity
     * @param cacheKey cache key，evict if not null
     * @param cacheKeys cache keys，evict if not null
     * @param database if null, use overridden db in subclass
     * @param w WhereDeclaration
     * @return affected rows count
     * */
    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> delete(
        meta: META,
        cacheKey: String? = null,
        cacheKeys: List<String>? = null,
        database: JdbcDatabase = db,
        w: WhereDeclaration
    ) =
        database.runQuery {
            QueryDsl.delete(meta).where(w)
        }.also {
            if (cacheKey != null) cache.evict(cacheKey) else if (!cacheKeys.isNullOrEmpty()) cacheKeys.forEach {
                cache.evict(it)
            }
        }


}