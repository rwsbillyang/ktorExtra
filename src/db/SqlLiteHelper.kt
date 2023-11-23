/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-10-09 18:00
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


import java.sql.*

/**
 * @param sqliteDbPath such as /user/root/mysqlite.db
 * eg:
val sqlLiteHelper = SqlLiteHelper("/user/root/mysqlite.db")
sqlLiteHelper.dropTable("Rule")
val sql = """
CREATE TABLE "Rule" (
"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
"label"	TEXT NOT NULL,
);
""".trimIndent()

sqlLiteHelper.createTable(sql)

val list = ... //data
list.forEach {
val map = mapOf<String, Any?>(
"id" to it.id,
"label" to it.label,
)
sqlLiteHelper.insert("Rule", map)

sqlLiteHelper.close()
 * */
class SqlLiteHelper(private val sqliteDbPath: String)
{
    val connection: Connection
    init {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:$sqliteDbPath")
        println("Connection to SQLite has been established.")
    }

    fun close(){
        try {
            connection.close()
        } catch (ex: SQLException) {
            println(ex.message)
        }
    }

    fun createTable(createTableSql: String){
        try {
            connection.createStatement().use { stmt ->
                stmt.execute(createTableSql)
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun dropTable(tableName: String){
        try {
            connection.createStatement().use { stmt ->
                stmt.execute("DROP TABLE $tableName;")
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    //
    /**
     * @param tableName
     * @param where eg:  capacity > ?
     * @param pstmtSetWhere eg: pstmt.setDouble(1,capacity);
     * @return ResultSet
    while (rs.next()) {
    println(
    rs.getInt("id").toString() + "\t" +
    rs.getString("name") + "\t" +
    rs.getDouble("capacity")
    )
    }
     * */
    fun findBuggy(tableName: String, whereExpr: String, pstmtSetWhere: (pstmt: PreparedStatement) -> Unit): ResultSet{
        val sql ="SELECT * FROM $tableName WHERE $whereExpr"
        connection.prepareStatement(sql).use { pstmt ->
            pstmtSetWhere(pstmt)
            return pstmt.executeQuery()
        }
    }
    fun find(tableName: String, wherePart: String) = connection.createStatement().executeQuery("SELECT * FROM $tableName $wherePart")
    fun findAll(tableName: String) = connection.createStatement().executeQuery("SELECT * FROM $tableName")
    fun find(sql: String) = connection.createStatement().executeQuery(sql)

    /**
     * @param tableName
     * @param valueMap key: field name, value: member value
     * */
    fun insert(tableName: String, valueMap: Map<String, Any?>){
        val partSql1 = valueMap.keys.joinToString(","){ "`$it`" }
        val partSql2 = valueMap.keys.joinToString(","){ "?" }
        val sql = "INSERT INTO $tableName($partSql1) VALUES($partSql2)" //"INSERT INTO $tableName(name,capacity) VALUES(?,?)"

        val entries = valueMap.values
        executePrepareStatement(sql){
            entries.forEachIndexed { index, v ->
                pstmtSetValue(it, index + 1,  v)
            }
        }
    }

    /**
     * @param tableName
     * @param id only support Int as primary key
     * @param valueMap key: field name, value: member value
     * */
    fun updateById(tableName: String, id: Int, valueMap: Map<String, Any?>){
        val entries = valueMap.entries
        val partSql = entries.joinToString(","){ " `${it.key}` = ?" }
        val sql = "UPDATE $tableName SET $partSql WHERE id = ?" //UPDATE $tableName SET name = ? , capacity = ?, WHERE id = ?"

        executePrepareStatement(sql){
            entries.forEachIndexed { index, v ->
                pstmtSetValue(it, index + 1,  v.value)
            }
            it.setInt(entries.size + 1, id)
        }
    }

    /**
     * @param tableName
     * @param id only support Int as primary key
     * */
    fun deleteById(tableName: String, id: Int){
        val sql = "DELETE FROM $tableName WHERE id = ?"
        executePrepareStatement(sql){
            it.setInt(1, id)
        }
    }

    /**
     * pstmt.setString(1, name)
     * pstmt.setDouble(2, capacity)
     * */
    fun executePrepareStatement(sql: String, pstmtSetValue: (pstmt: PreparedStatement) -> Unit){
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmtSetValue(pstmt)
                pstmt.executeUpdate()
            }
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun pstmtSetValue(pstmt: PreparedStatement, index: Int, value: Any?){
        when(value){
            is Boolean -> pstmt.setBoolean(index, value)
            is Int -> pstmt.setInt(index, value)
            is Long -> pstmt.setLong(index, value)
            is Float -> pstmt.setFloat(index, value)
            is Double -> pstmt.setDouble(index, value)
            is String -> pstmt.setString(index, value)
            is Date -> pstmt.setDate(index, value)
            is Timestamp -> pstmt.setTimestamp(index, value)
            is ByteArray -> pstmt.setBytes(index, value)
            is Byte -> pstmt.setByte(index, value)
        }
    }
}