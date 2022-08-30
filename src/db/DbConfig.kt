/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-07-24 21:38
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

enum class DbType{NOSQL, SQL}

class DbConfig(
    val dbName: String,
    val dbType: DbType = DbType.NOSQL,
    val host: String = "127.0.0.1",
    val port: Int = when(dbType){
        DbType.NOSQL -> 27017
        DbType.SQL -> 3306
    },
    val userName: String? = null,
    val pwd: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        return if (other is DbConfig) {
            other.dbName == dbName && other.dbType == dbType && other.host == host && other.port == port
        } else
            false

    }

    override fun hashCode(): Int {
        var result = dbName.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        return result
    }
}
