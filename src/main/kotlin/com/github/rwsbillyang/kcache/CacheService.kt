/*
 * Copyright © 2019 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2019-11-05 15:51
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

package com.github.rwsbillyang.kcache


import org.koin.core.KoinComponent
import org.slf4j.LoggerFactory

/**
 * 在执行后，会将其返回结果进行缓存起来，以保证下次同样参数来执行该方法的时候可以从缓存中返回结果，而不需要再次执行此方法。
 * 可以指定三个属性：key、call,condition
 * 通常用于find方法
 * */
inline fun <reified T: Any> Cacheable(init: CacheServiceDsl.() -> Unit): T? {
    val cacheService = CacheServiceDsl()
    cacheService.init()
    val data = cacheService.doCacheable()
    return if(data == null)  return null else data as? T
}
/**
 * 与Cacheable不同，它虽然也可以声明一个方法支持缓存，但它执行方法前是不会去检查缓存中是否存在之前执行过的结果，
 * 而是每次都执行该方法，并将执行结果放入指定缓存中。
 *
 * 通常用于save方法
 * */
inline fun <reified T: Any> CachePut(init: CacheServiceDsl.() -> Unit): T?{
    val cacheService = CacheServiceDsl()
    cacheService.init()
    val data = cacheService.doCachePut()
    return if(data == null)  return null else data as? T
}
/**
 * 可以指定的属性有key、condition、allEntries和beforeInvocation。
 *
 * key是表示需要清除的是哪个key。
 * -allEntries是表示是否需要清除缓存中所有的元素。
 *
 * */
inline fun <reified T: Any> CacheEvict (init: CacheServiceDsl.() -> Unit): T?{
    val cacheService = CacheServiceDsl()
    cacheService.init()
    val data =  cacheService.doCacheEvict()
    return if(data == null)  return null else data as? T
}




/**
 * 操纵缓存的类
 * */
open class CacheServiceDsl()
{
    companion object{
        private val log = LoggerFactory.getLogger("CacheService")
    }


    /**
     * 是否激活log输出，默认为false
     * */
    var enableLog = false;
    /**
     * cache,可以自己重新指定，不指定的话不进行任何缓存操作
     * */
    var cache: ICache? = null

    /**
     * 缓存的键
     * */
    var key: Any? = null

    /**
     * beforeInvocation清除操作默认是在方法成功执行之后触发的 默认true
     * */
    //var beforeInvocation: Boolean = true



    private var callBlock: (() -> Any?)? = null


    private var conditionBlock: (Any?) -> Boolean = {true}


    private var allEntriesBlock: ((Any?) -> List<Any>?)? = null


    /**
     * 从数据库查询数据的代码块
     * */
    fun call(block: (() -> Any?)?)
    {
        callBlock = block
    }

    /**
     * 执行条件，为true时执行操作
     * */
    fun condition(block: (Any?) -> Boolean)
    {
        conditionBlock = block
    }

    fun allEntries(block: (Any?) -> List<Any>?)
    {
        allEntriesBlock = block
    }

    /**
     * 若不使用缓存的话，直接从DB中加载；否则，试图从缓存加载数据，若存在直接返回，不存在则从DB中加载并保存到缓存中。
     *
     * 当从DB中加载的值是空值时，将把NullValue保存到缓存中作为DB中没有的标记，不过这发生在condition成立时。
     * */
    fun doCacheable():Any?
    {

        if(key == null)
        {
            log.error("key is null")
        }
        val key = key?: "nokey"

        var value: Any?  = cache?.get(key)
        if(value != null)
        {
            if(enableLog) log.info("to find data in cache...key=$key")
            return if(value is NullValue){
                if(enableLog) log.warn("no value in DB and cache,key=$key")
                null
            }else
                value
        }

        //log.info("prepare callBlock...")
        callBlock?.let {
            value = callBlock!!()
        }

        if(conditionBlock(value))
        {
            cache?.put(key, value?: NullValue())
        }

        return value
    }

    /**
     * 在condition成立的情况下，直接把结果放到缓存中
     * */
    fun doCachePut():Any?{
        if(key == null)
        {
            log.error("key is null")
        }
        val key = key?: "nokey"

        if(enableLog) log.info("doCachePut...key=$key")
        var value: Any? = null
        callBlock?.let {
            value = callBlock!!()
        }

        if(conditionBlock(value))
        {
            cache?.put(key, value?: NullValue())
        }

        return value
    }

    fun doCacheEvict():Any?{
        if(enableLog) log.info("doCacheEvict...key=$key")
        val value:Any? = callBlock?.let{ callBlock!!()}

        if(conditionBlock(value)){
            key?.let{ cache?.evict(key!!) }
            allEntriesBlock?.let{ it(value)?.forEach { cache?.evict(it) }}
        }

        return value
    }
}

/**
 * 非DSL版本，简化实现，直接注入后进行调用
 * */
open class CacheService(private val cache: ICache): KoinComponent
{
    /**
     * 首先检查缓存，缓存中若存在则直接返回(可能为null)；否则返回block执行后的返回值(可能为null)
     * */
    fun <T> cacheable(key: String, block: () -> T?): T?
    {
        var value: T?  = cache[key] as? T
        if(value != null && value is NullValue)
        {
            value = null
        }else{
            value = block()
            cache.put(key, value?: NullValue())
        }
        return value
    }
    /**
     * 返回block执行后的返回值
     * */
    fun <T> put(key: String, block: () -> T?): T?
    {
        val value: T? = block()
        cache.put(key, value?: NullValue())
        return value
    }
    /**
     * 返回block执行后的返回值
     * */
    fun <T> evict(key: String, block: () -> T): T
    {
        cache.evict(key)
        return block()
    }
    /**
     * 返回block执行后的返回值
     * */
    fun <T> batchEvict(keys: List<String>, block: () -> T): T
    {
        keys.forEach { cache.evict(it) }
        return block()
    }
}