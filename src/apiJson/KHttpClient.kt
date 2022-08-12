/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.ktorKit.apiJson


import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*

import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*

import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.http.*

import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File



open class KHttpClient {
    companion object {
        val apiJson = Json {
            encodeDefaults = false //变为false 会影响某些非空默认值，如模板消息颜色
            useArrayPolymorphism = false
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }

        var logLevel = LogLevel.INFO //使用者可直接配置

        val DefaultClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(apiJson)
            }
            install(HttpCache)
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 20000
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
//            install(JsonFeature) {
//                serializer = KotlinxSerializer(apiJson)
//                accept(ContentType.Application.Json)
//                //OAuthApi.getAccessToken 返回的是：Content-Type: text/plain
//                accept(ContentType.Text.Any) //https://github.com/ktorio/ktor/issues/772
//            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = logLevel
            }
            defaultRequest { // this: HttpRequestBuilder ->
                contentType(ContentType.Application.Json)
            }

            //https://ktor.io/docs/http-client-engines.html#jvm-and-android
            engine {
                maxConnectionsCount = 20480

                endpoint {
                    /**
                     * Maximum number of requests for a specific endpoint route.
                     */
                    maxConnectionsPerRoute = 10240

                    /**
                     * Max size of scheduled requests per connection(pipeline queue size).
                     */
                    pipelineMaxSize = 20

                    /**
                     * Max number of milliseconds to keep idle connection alive.
                     */
                    keepAliveTime = 5000

                    /**
                     * Number of milliseconds to wait trying to connect to the server.
                     */
                    connectTimeout = 8000

                    /**
                     * Maximum number of attempts for retrying a connection.
                     */
                    connectAttempts = 3
                }
            }
        }
    }

    /**
     * 多数情况下使用默认的client即可,亦可定制自己的client
     * */
    open val client = DefaultClient
    fun getByRaw(url: String): HttpResponse = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.get(url)
        }
    }
    inline fun <reified R> get(url: String): R = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.get(url).body()
        }
    }

    /**
     * 返回R泛型类型结果
     * */
    inline fun <reified T, reified R> post(url: String, data: T? = null): R = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post(url) {
                data?.let { setBody(data) }
            }.body()
        }
    }
    inline fun <reified T> postByRaw(url: String, data: T? = null): HttpResponse = runBlocking {
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post(url) {
                data?.let { setBody(data) }
            }
        }
    }

    inline fun <reified R> upload(
        url: String,
        filePath: String,
        formData: Map<String, String>? = null,
        fileKey: String = "media",
        contentType: String = "image/*",
        boundary: String = "fileBoundary"
    ):R = runBlocking {
        //https://ktor.io/docs/request.html#upload_file
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            client.post(url) {
                val file = File(filePath)
//               MultiPartFormDataContent(formData {
//                    formData?.forEach { append(it.key, it.value) }
//                    appendInput("media", size = file.length()) {
//                        FileInputStream(file).asInput()
//                    }
//                })

                setBody(
                    MultiPartFormDataContent(
                        formData {
                            formData?.forEach { append(it.key, it.value) }
                            append(fileKey, file.readBytes(), Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                            })
                        },
                        boundary = boundary
                    )
                )
                onUpload { bytesSentTotal, contentLength ->
                    println("Sent $bytesSentTotal bytes from $contentLength")
                }
            }.body()
        }
    }

    inline fun <reified R>  upload(
        url: String,
        filePath: String,
        formData: Map<String, String>? = null,
        fileKey: String = "media",
        contentType: String = "image/*"
    ):R = runBlocking {
        //https://ktor.io/docs/request.html#upload_file
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val file = File(filePath)
            client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    formData?.forEach { append(it.key, it.value) }
                    append(fileKey, file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                    })
                }
            ).body()
        }
    }

    fun uploadByRaw(
        url: String,
        filePath: String,
        formData: Map<String, String>? = null,
        fileKey: String = "media",
        contentType: String = "image/*"
    ) = runBlocking {
        //https://ktor.io/docs/request.html#upload_file
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val file = File(filePath)
            client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    formData?.forEach { append(it.key, it.value) }
                    append(fileKey, file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                    })
                }
            )
        }
    }
    /**
     * 根据url下载文件，保存到filepath中
     *
     * @param url
     * @param filepath such as: ./qrcode/${appId}
     * @param filename: such as: abc.jpg
     * @return 成功返回true，否则返回失败
     */
    fun download(url: String, filepath: String, filename: String) = runBlocking {
        client.prepareGet(url).execute { httpResponse ->
            if(httpResponse.status.isSuccess()){
                try {
                    val channel: ByteReadChannel = httpResponse.body()
                    val file = File("$filepath/$filename")
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            file.appendBytes(bytes)
                            //println("Received ${file.length()} bytes from ${httpResponse.contentLength()}")
                        }
                    }
                    //println("A file saved to ${file.path}")
                    true
                }catch (e: Exception){
                    println("save $filename fail! Exception: ${e.message}")
                    false
                }
            }else false

        }

//        val response: HttpResponse = client.get(url)
//        if (response.status.isSuccess()) {
//            val content = ByteArrayContent(response.readBytes())
//            val path = File(filepath)
//            if (!path.exists()) {
//                path.mkdirs()
//            }
//            val file = File("$filepath/$filename")
//            file.writeBytes(content.bytes())
//            true
//        } else {
//            println("fail to download from $url, status=${response.status.value}")
//            false
//        }
    }


}