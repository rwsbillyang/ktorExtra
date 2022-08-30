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



import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.configuration

import com.github.jershell.kbson.Configuration
import org.koin.core.component.KoinComponent

//http://litote.org/kmongo/
class MongoDataSource(dbName: String, host: String = "127.0.0.1", port: Int = 27017) : KoinComponent {
    val mongoDb: CoroutineDatabase

    init {
        //mongodb://[username:password@]host1[:port1][,...hostN[:portN]]][/[database][?options]]
        val client = KMongo.createClient("mongodb://${host}:${port}/${dbName}").coroutine
        mongoDb = client.getDatabase(dbName)


        //Null properties are taken into account during the update (they are set to null in the document
        // in MongoDb). If you prefer to ignore null properties during the update, you can use the
        // updateOnlyNotNullProperties parameter:
        //UpdateConfiguration.updateOnlyNotNullProperties = true


        //registerModule(serializersModuleOf(ObjectId::class, ObjectIdSerializer))
        //registerSerializer(MyReMsgSerializer)
        configuration =  Configuration(
            encodeDefaults = false,
            //默认___type, 此处修改不生效 kserializex/kbson对 ___type位置有要求，一般是第一个字段，有时是最后一个字段
            //可能存在不兼容的情况，即反序列化时会报错
            //classDiscriminator = "_class",
            nonEncodeNull = true
        )
    }
}
