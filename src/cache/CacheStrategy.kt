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



/**
 * 暂时用不上，没有实现，只使用的是CacheAside
 * */
enum class CacheStrategy{
    /**
     * This is where application is responsible for reading and writing from the database and the cache doesn't interact
     * with the database at all. The cache is "kept aside" as a faster and more scalable in-memory data store.
     * The application checks the cache before reading anything from the database. And, the application updates the cache
     * after making any updates to the database. This way, the application ensures that the cache is kept synchronized
     * with the database.
     *
     * application读前先检查cache中是否存在，若存在直接返回，不存在，则application从后端中去读；写时，application先写入后端backend，再更新缓存。
     * */
    CacheAside,

    /**
     * application与cache交互，cache中有数据则直接返回，若没有则从后端读取后再返回
     * */
    ReadThrough,
    /**
     * application与cache交互，更新cache，然后cache再去更新后端，最后返回
     * */
    WriteThrough,

    /**
     * TODO： 待实现
     * 在ache数据过期前，自动重新从backend（如数据库）加载数据。查询数据时，自动重载数据的时间可以设定。
     * */
    RefreshAhead,

    /**
     * cache进行update后，直接返回结果。再异步更新backend，
     * TODO：间隔一段时间后(interval)去更新backend
     * */
    WriteBehind

}
