/*
 * Copyright © 2019 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2019-11-05 15:54
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

/**
 * null值，避免缓存击穿
 * */
class NullValue{
    override fun equals(other: Any?): Boolean {
        return this === other || other == null
    }

    override fun hashCode(): Int {
        return NullValue::class.hashCode()
    }

    override fun toString(): String {
        return "null"
    }
}