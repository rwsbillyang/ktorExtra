/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:34
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

package com.github.rwsbillyang.ktorKit


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object ApiJson {
    @OptIn(ExperimentalSerializationApi::class)
    fun JsonBuilder.apiJsonBuilder() {
        encodeDefaults = true
        explicitNulls = false

        ignoreUnknownKeys = true
        classDiscriminator = "_class" //某些payload中拥有type字段，会冲突
        //isLenient = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = false

    }

    /**
     * server侧的serialize，不包含ObjectId和LocalDateTime的自定义序列化
     * 忽略null值，忽略未知键等，具体如下：
     * encodeDefaults = true
     * explicitNulls = false
     * ignoreUnknownKeys = true
     * classDiscriminator = "_class" //某些payload中拥有type字段，会冲突
     * allowSpecialFloatingPointValues = true
     * useArrayPolymorphism = false
     * */
    val serializeJson = Json {
        apiJsonBuilder()
    }

    /**
     * server侧的serialize，包含了ObjectId和LocalDateTime的自定义序列化
     * */
    val serverSerializeJson = Json {
        apiJsonBuilder()
        serializersModule = SerializersModule {
            try{
                Class.forName("org.bson.types.ObjectId", false, javaClass.classLoader)
                contextual(ObjectIdBase64Serializer)
            }catch (e: Exception){
                System.err.println("no bson dependency, ignore ObjectIdBase64Serializer")
            }

            //contextual(LocalDateTimeAsStringSerializer)
            contextual(LocalDateTimeAsLongSerializer)
        }
    }


    /**
     * 用于client侧的deserialize，等同于apiJsonBuilder
     * client发送请求数据，接受结果数据的序列化
     * */
    val clientApiJson = Json {
        apiJsonBuilder()
    }

}