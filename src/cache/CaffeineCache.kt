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

import com.github.benmanes.caffeine.cache.Caffeine
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit


/**
 *
 * CaffeineCache, 具体使用哪种cache取决于app中注入
 *
 * expire 10 minutes after write or read
 * */
open class CaffeineCache(maxEntries:Long = 10000L,
                         expireAfterWriteSeconds:Long = 600L,
                         expireAfterAccessSeconds:Long = 600L): ICache, KoinComponent {

    private val cache = Caffeine.newBuilder()
        .maximumSize(maxEntries)
        .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
        .expireAfterAccess(expireAfterAccessSeconds, TimeUnit.SECONDS)
        //.refreshAfterWrite(1, TimeUnit.MINUTES)
        .build<Any, Any>()

    override fun get(key: Any): Any? {
        return cache.getIfPresent(key)
    }

    override fun put(key: Any, value: Any) {
        cache.put(key,value)
    }

    override fun evict(key: Any) {
        cache.invalidate(key)
    }

    override fun clear() {
        cache.cleanUp()
    }
}
