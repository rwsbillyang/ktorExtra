package com.github.rwsbillyang.ktorExt

import com.github.rwsbillyang.apiJson.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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

suspend inline fun <reified T> ApplicationCall.respondBox(box: DataBox<T>) =
    respondText(ApiJson.json2.encodeToString(box), ContentType.Application.Json, HttpStatusCode.OK)


/**
 * @param uploadParentDir 存储于何处
 * @return 返回上传的文件名称，若为空，则表示失败
 *
 * https://ktor.io/docs/uploads.html#receiving-files-using-multipart
 * https://github.com/ktorio/ktor-samples/blob/1.3.0/app/youkube/src/Upload.kt
 * */
suspend fun ApplicationCall.handleUpload(uploadParentDir: String):String?
{
    val uId: String? = this.uId
    if(uId.isNullOrBlank()){
        throw AuthorizationException()
    }

    val uploadDir = File(uploadParentDir)
    if (!uploadDir.exists()  && !uploadDir.mkdirs()) {
        throw IOException("Failed to create directory ${uploadDir.absolutePath}")
    }

    var fileName: String? = null

    this.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val ext = File(part.originalFileName).extension
                fileName = "$uId-${System.currentTimeMillis()}.$ext"
                val file = File(uploadDir, fileName)
                part.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
            }
            else -> {println("not support part type")}
        }
        part.dispose()
    }

    return fileName
}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}