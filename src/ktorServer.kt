/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-01-21 17:23
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


import com.github.rwsbillyang.ktorKit.apiJson.ApiJson
import com.github.rwsbillyang.ktorKit.apiJson.Code
import com.github.rwsbillyang.ktorKit.apiJson.DataBox
import com.github.rwsbillyang.ktorKit.apiJson.UmiBox
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.encodeToString


class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

class BizException(
    val code: String, val msg: String? = null,
    val type: Int? = null, val tId: String? = null, val host: String? = null,
    cause: Throwable? = null
) : RuntimeException(cause)
{
    companion object{
        fun ko(msg: String, type: Int? = UmiBox.WARN_MESSAGE, tId: String? = null, host: String? = null) = BizException(
            Code.KO,
            msg,
            type,
            tId,
            host
        )
    }
}

fun Routing.exceptionPage()
{
    install(StatusPages) {
        exception<AuthenticationException> { cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<BizException> { e ->
            call.respond(UmiBox(e.code, e.msg, e.type, e.tId, e.host))
        }
    }
}


suspend inline fun <reified T> ApplicationCall.respondBox(box: DataBox<T>) =
    respondText(ApiJson.json2.encodeToString(box), ContentType.Application.Json, HttpStatusCode.OK)







