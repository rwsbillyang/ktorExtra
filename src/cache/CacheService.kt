/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-21 17:19
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

package com.github.rwsbillyang.ktorKit.cache



import org.koin.core.component.KoinComponent


/**
 * 非DSL版本，简化实现，直接注入后进行调用
 * */
open class CacheService(val cache: ICache): KoinComponent
{
    /**
     * 不空才缓存
     * */
    fun <T: Any> cacheable(key: String, block: () -> T?): T?
    {
        val value: T? = cache[key] as? T
        return value?:block()?.also { cache.put(key, it) }
    }

    /**
     * 返回block执行后的返回值, 若返回空不缓存
     * */
    fun <T: Any> cache(key: String, block: () -> T): T
    {
        return block().also { cache.put(key, it) }
    }

    /**
     * 缓存任何值，包括空值，用于只查询一次的情况，
     * */
    fun <T: Any> cacheIncludeNull(key: String, block: () -> T?): T?
    {
        var value: T?  = cache[key] as? T
        return if(value != null) {
            if(value is NullValue)
                null
            else
                value
        }else{
            value = block()
            cache.put(key, value?: NullValue())
            value
        }
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