package com.github.rwsbillyang.ktorExt


import com.github.rwsbillyang.data.DataSource
import com.github.rwsbillyang.apiJson.ApiJson
import com.github.rwsbillyang.kcache.CaffeineCache
import com.github.rwsbillyang.kcache.ICache
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.slf4j.event.Level

/**
 * @param modules 需要注入的实例的模块列表
 * @param dbName 数据库名称，模块默认提供，为null，则不注入DataSource；installAppModule若提供时将覆盖它
 * @param routing route api
 * */
class AppModule(
    val modules: List<Module>?,
    var dbName: String?,
    val routing: (Routing.() -> Unit)? = null
)

class DbConfig(
    val dbName: String,
    val host: String = "127.0.0.1",
    val port: Int = 27017
) {
    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        return if (other is DbConfig) {
            other.dbName == dbName && other.host == host && other.port == port
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

private val _dbConfigSet = mutableSetOf<DbConfig>()
private val _MyKoinModules = mutableListOf<Module>()
private val _MyRoutings = mutableListOf<Routing.() -> Unit>()

/**
 * 安装appModule，实际完成功能是
 * （1）将待注入的koin module添加到一个私有全局列表，便于defaultInstall中进行 install(Koin)
 * （2）将routing配置加入私有全局列表，便于后面执行，添加endpoint
 * （3）自动注入了DataSource（以数据库名称作为qualifier）
 * @param app 待安装的module
 * @param dbName 数据库名称，不指定则使用AppModule中的默认名称
 * @param host 数据库host 默认127.0.0.1
 * @param port 数据库port 默认27017
 * */
fun Application.installModule(
    app: AppModule,
    dbName: String? = null,
    host: String = "127.0.0.1",
    port: Int = 27017
) {
    dbName?.let { app.dbName = it }
    app.dbName?.let {
        _dbConfigSet.add(DbConfig(it, host, port))
    }

    app.modules?.let { _MyKoinModules.plusAssign(it) }
    app.routing?.let { _MyRoutings.plusAssign(it) }

}


/**
 * @param jsonBuilderAction 添加额外的自定义json配置，通常用于添加自己的json contextual
 *
 * 自动注入 CaffeineCache，
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.defaultInstall(
    testing: Boolean = false,
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null
) {
    val module = module {
        single<ICache> { CaffeineCache() }
        _dbConfigSet.forEach {
            val config = it
            single(named(it.dbName)) { DataSource(config.dbName, config.host, config.port) }
        }
    }

    _MyKoinModules.add(0, module)
    install(Koin) {
        modules(_MyKoinModules)
    }
    log.info("_MyKoinModules.size=${_MyKoinModules.size}")

    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
    //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
    install(ContentNegotiation) {
        json(
            json = if (jsonBuilderAction == null) ApiJson.json2 else Json(ApiJson.json2, jsonBuilderAction),
            contentType = ContentType.Application.Json
        )
    }


    install(Locations)


    val jwtHelper: AbstractJwtHelper by inject()
    install(Authentication) {
        jwt {
            config(jwtHelper)
        }
    }


    _MyRoutings.add {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
        //convenience for test api
        get("/api/hello") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
    }
    _MyRoutings.add {
        exceptionPage()
    }
    log.info("_MyRoutings.size=${_MyRoutings.size}")

    _MyRoutings.forEach {
        routing {
            it()
        }
    }
}


fun Application.testModule(module: AppModule) {
    val app = this
    installModule( AppModule(
        listOf(module(createdAtStart = true) {
            single<UserInfoJwtHelper> { TestJwtHelper() }
            single<AbstractJwtHelper> { DevJwtHelper() }
            single<Application> { app }
        }), null), null)
    installModule(module)
    defaultInstall(true)
}
