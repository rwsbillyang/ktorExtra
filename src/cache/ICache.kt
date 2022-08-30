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


interface ICache
{
    // cacheName，缓存的名字
    //var name: String?

    // 通过key获取缓存值，返回的是实际值，即方法的返回值类型
    operator fun get(key: Any): Any?

    // 将@Cacheable注解方法返回的数据放入缓存中
    fun put(key: Any, value: Any)

    // 删除缓存
    fun evict(key: Any)

    // 删除缓存中的所有数据。需要注意的是，具体实现中只删除使用@Cacheable注解缓存的所有数据，不要影响应用内的其他缓存
    fun clear()
}