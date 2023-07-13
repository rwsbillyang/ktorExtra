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

import com.github.rwsbillyang.ktorKit.cache.CacheService
import com.github.rwsbillyang.ktorKit.cache.ICache
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.AssignmentDeclaration
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.operator.and
import org.komapper.core.dsl.query.singleOrNull
import org.komapper.jdbc.JdbcDatabase

class SqlPagination(
    val sort: SortExpression,
    val pageSize: Int = 10, // -1 表示全部
    val offset: Int = 0 //(pagination.current - 1) * pagination.pageSize
){
    var where: WhereDeclaration? = null
    fun addWhere(vararg arrays: WhereDeclaration?):SqlPagination
    {
        val list = arrays.filterNotNull()
        if(list.isNotEmpty()){
            where = list.reduce{e1, e2 -> e1.and(e2)}
        }
        return this
    }
}

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
abstract class AbstractSqlService(cache: ICache) : CacheService(cache) {
    abstract val dbSource: SqlDataSource
    val db: JdbcDatabase
        get() = dbSource.db
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
        w: WhereDeclaration,
        cacheKey: String? = null,
        database: JdbcDatabase = db,
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
        w: WhereDeclaration ? = null,
        database: JdbcDatabase = db
    ) =
        database.runQuery {
            if(w == null)
                QueryDsl.from(meta)
            else
                QueryDsl.from(meta).where(w)
        }


    fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> findPage(
        meta: META,
        pagination: SqlPagination,
        database: JdbcDatabase = db) = database.runQuery{
        if(pagination.pageSize == -1){//若pageSize为-1则表示某些条件下的全部数据
            (pagination.where?.let { QueryDsl.from(meta).where(it) }?:QueryDsl.from(meta))
                .orderBy(pagination.sort)
        }else{
            (pagination.where?.let { QueryDsl.from(meta).where(it) }?:QueryDsl.from(meta))
                .orderBy(pagination.sort)
                .offset(pagination.offset)
                .limit(pagination.pageSize)
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
        w: WhereDeclaration,
        cacheKey: String? = null,
        cacheKeys: List<String>? = null,
        database: JdbcDatabase = db
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
        w: WhereDeclaration,
        cacheKey: String? = null,
        cacheKeys: List<String>? = null,
        database: JdbcDatabase = db
    ) =
        database.runQuery {
            QueryDsl.delete(meta).where(w)
        }.also {
            if (cacheKey != null) cache.evict(cacheKey) else if (!cacheKeys.isNullOrEmpty()) cacheKeys.forEach {
                cache.evict(it)
            }
        }

}
open class SqlGenericService(dbName: String, cache: ICache) : AbstractSqlService(cache) {
    override val dbSource: SqlDataSource by inject(qualifier = named(dbName))

}
