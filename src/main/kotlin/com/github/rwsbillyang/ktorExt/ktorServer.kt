package com.github.rwsbillyang.ktorExt

import com.github.rwsbillyang.apiJson.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.encodeToString
import java.lang.RuntimeException

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

class BizException(val code: String, val msg: String? = null,
                   val type: Int? = null , val tId: String? =null, val host: String? = null,
                   cause: Throwable? = null ) : RuntimeException(cause)
{
    companion object{
        fun ko(msg: String, type: Int ? = UmiBox.WARN_MESSAGE, tId: String? = null, host: String? = null) = BizException(Code.KO, msg, type,tId,host)
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
            call.respond(UmiBox(e.code, e.msg, e.type,e.tId,e.host))
        }

    }
}

suspend inline fun <reified T> ApplicationCall.respondT(box: DataBox<T>) =
    respondText(ApiJson.json2.encodeToString(box), ContentType.Application.Json, HttpStatusCode.OK)