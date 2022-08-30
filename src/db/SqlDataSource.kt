/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-27 15:07
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

import com.zaxxer.hikari.HikariDataSource
import org.koin.core.component.KoinComponent
import org.komapper.dialect.mysql.jdbc.MySqlJdbcDialect
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialect

class SqlDataSource(dbName: String, host: String ="localhost", port: Int = 3306,
                    userName: String? = null, pwd: String? = null,
                    jdbcDialect: JdbcDialect? = null): KoinComponent {
    val db: JdbcDatabase = JdbcDatabase(
        dataSource = HikariDataSource(optimizedHikariConfig(dbName,userName?:"root",pwd,host,port)),
        dialect = jdbcDialect?:MySqlJdbcDialect()
    )
}