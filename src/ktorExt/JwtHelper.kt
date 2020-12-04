/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-09-14 18:25
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.ktorExt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.interfaces.Payload
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.util.*
import org.apache.commons.lang3.RandomStringUtils
import org.koin.core.KoinComponent
import org.slf4j.LoggerFactory
import java.util.*

/**
 * webapp登录时收到token之后，亦需将uId设置到header "X-Auth-uId"之中
 * */
val ApplicationCall.uId
    get() = this.request.headers["X-Auth-uId"]

val ApplicationCall.oId
    get() = this.request.headers["X-Auth-oId"]

val ApplicationCall.unionId
    get() = this.request.headers["X-Auth-unId"]

//val ApplicationCall.uId: String
//    get() = this.authentication.principal<JWTPrincipal>()!!.payload.getClaim(JwtHelper.KeyUID).asString()


var ApplicationCall.authInfo: AuthUserInfo
    get() = this.attributes.take(AuthUserInfoKey)
    set(value) = this.attributes.put(AuthUserInfoKey,value)

// Declared as a global property
private val AuthUserInfoKey = AttributeKey<AuthUserInfo>("AuthUserInfo")




/**
 *
 * */
enum class Role { guest, user, admin, root }

/**
 * 暂只有读写两样权限
 * */
enum class Action { READ, WRITE, ALL }



interface IAuthUserInfo
{
    val uId: String
    companion object {
        const val KEY_UID = "uId"
    }
}

/**
 * 从token解码出来的用户认证信息
 * */
class AuthUserInfo(
    override val uId: String,
    val level: Int?,
    val role: List<String>?,
): IAuthUserInfo
{
    /**
     * 最终结果，为null表示未定
     * */
    var isAllow: Boolean? = null

    companion object {
        const val KEY_LEVEL = "level"
        const val KEY_ROLE = "role"
    }
}


fun JWTAuthenticationProvider.Configuration.config(jwtHelper: AbstractJwtHelper) {
    verifier(jwtHelper.verifier())
    this.realm = jwtHelper.realm
    validate { credential -> jwtHelper.validate(credential) }
}


/**
 *
 * @param secretKey 秘钥，如RSA秘钥
 * @param issuer 填写自己的域名
 * @param realm 比如xxxServer
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
abstract class AbstractJwtHelper(
    private val issuer: String,
    secretKey: String,
    val realm: String = "Server",
    private val audience: String = "webapp",
    private val subject: String = "server",
    private val expireDays: Int = 90
): KoinComponent
{
    protected val log = LoggerFactory.getLogger("AbstractJwtHelper")

    private val algorithm: Algorithm = Algorithm.HMAC256(secretKey)

    /**
     * 对JWTCredential.Payload进行校验
     * */
    abstract fun validate(payload: Payload):Boolean
    /**
     * 判断对某个call请求操作是否有权限
     * @return 具备操作权限返回true，否则返回false; 返回null时表示TBD待决定(适合于数据需要owner时的情况)
     *
     * @param call 操作请求
     * @param action 操作类型
     * @param subject 操作对象
     * */
    abstract fun isAuthorized(call: ApplicationCall, action: Action? = null, subject: String? = null): Boolean?


    fun validate(credential: JWTCredential): Principal? {
        return if(validate(credential.payload))
             JWTPrincipal(credential.payload)
        else
            null
    }

    /**
     * install Authentication 需要
     * */
    fun verifier(): JWTVerifier {
        val v = JWT.require(algorithm)
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)

        //claims.forEach{ v.withClaim(it.key,it.value) }

        return v.build() //Reusable verifier instance
    }



    /**
     * @param jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
     * @param claims 需要添加的键值对
     * */
    fun generateToken(jti: String, claims: HashMap<String, String>? = null): String? {
        return try {
            val builder = jwtBuilder(jti)

            claims?.forEach { builder.withClaim(it.key, it.value) }

            builder.sign(algorithm)
        } catch (exception: JWTCreationException) {
            log.warn("Invalid Signing configuration / Couldn't convert Claims? JWTCreationException: ${exception.message}")
            null
        }
    }

    /**
     * 使用jwtBuilder之后，再withClaim之后，最后生成token
     * ```kotlin
     *     val jti = RandomStringUtils.randomAlphanumeric(8)
     *
     *     val token = jwtHelper.generateToken(jti){
     *          withClaim(JwtHelper.KeyUID, user._id!!.toHexString())
     *          withClaim(JwtHelper.KeyLevel, user.level)
     *     }
     * ```
     * */
    fun generateToken(jti: String, func: JWTCreator.Builder.() -> JWTCreator.Builder = { this }): String =
        jwtBuilder(jti).func().sign(algorithm)


    /**
     * 用于构建一个JWT，然后用于withClaim
     * <code>
     *     val jti = RandomStringUtils.randomAlphanumeric(8)
     *     val uId = user._id!!.toHexString()
     *     val token = jwtHelper
     *     .createToken(jwtHelper.jwtBuilder(jti)
     *     .withClaim(JwtHelper.KeyUID, uId)
     *     .withClaim(JwtHelper.KeyLevel, user.level))
     * </code>
     * */
    private fun jwtBuilder(jti: String): JWTCreator.Builder {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expireDays * 24 * 3600000L))
            .withJWTId(jti)
    }
}

abstract class UIdJwtHelper(issuer: String,
                     secretKey: String,
                     realm: String = "Server",
                     audience: String = "webapp",
                     subject: String = "server",
                     expireDays: Int = 90)
    : AbstractJwtHelper(secretKey, issuer, realm, audience, subject, expireDays)
{
    override fun validate(payload: Payload): Boolean {
        val claim = payload.getClaim(IAuthUserInfo.KEY_UID)
        if (claim.isNull) {
            log.info("no claim for ${IAuthUserInfo.KEY_UID} in jwtAuthentication")
            return false
        }

        val uId = claim.asString()
        if (!isAuthentic(uId, payload)) {
            log.info("no user or uId($uId) is disabled")
            return false
        }

        return true
    }

    /**
     * 判断是否是真正的用户，根据用户Id判断用于是否是真正的用户
     *
     * @param uId  用户id
     * @return 真正的用户返回true，否则返回false
     *
     * Example code:
     * ```koltin
     * if(!ObjectId.isValid(uId)){ return false }
     * val user = userService.findById(uId)
     * return user != null && user.status != User.StatusDisabled
     * ```
     * */
    abstract fun isAuthentic(uId: String, payload: Payload): Boolean
}
/**
 * 写入了uId/level/role等信息的jwt token helper类
 *
 * @param secretKey 秘钥，如RSA秘钥
 * @param issuer 填写自己的域名
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
abstract class UserInfoJwtHelper(
    issuer: String,
    secretKey: String,
    realm: String = "Server",
    audience: String = "webapp",
    subject: String = "server",
    expireDays: Int = 90
) : UIdJwtHelper(secretKey, issuer, realm, audience, subject, expireDays)
{
    fun generateToken(authUserInfo: AuthUserInfo) = generateToken(authUserInfo.uId, authUserInfo.level,authUserInfo.role)

    fun generateToken(uId: String, level: Int?, role: List<String>?): String {
        val jti = RandomStringUtils.randomAlphanumeric(8)
        return generateToken(jti) {
            withClaim(IAuthUserInfo.KEY_UID, uId)
            if (level != null) withClaim(AuthUserInfo.KEY_LEVEL, level.toString())
            if (!role.isNullOrEmpty()) withClaim(AuthUserInfo.KEY_ROLE, role.joinToString(","))
            this
        }
    }
    /**
     * 判断用户是否对某个请求操作有权限
     * @return 具备操作权限返回true，否则返回false; 返回null时表示TBD待决定(适合于数据需要owner时的情况)
     *
     * @param call 操作请求
     * @param action 操作类型
     * @param subject 操作对象
     *
     * */
    override fun isAuthorized(call: ApplicationCall, action: Action?, subject: String?): Boolean? {
        val payload = call.authentication.principal<JWTPrincipal>()?.payload
        if (payload == null) {
            log.warn("payload is null")
            return false
        }

        val uIdClaim = payload.getClaim(IAuthUserInfo.KEY_UID)
        if (uIdClaim.isNull) {
            log.warn("uIdClaim is null")
            return false
        }

        val uId: String = uIdClaim.asString()
        if(call.uId != uId){
            log.warn("uId not set in X-Auth-uId? should be same as one in token")
            return false
        }

        val levelClaim = payload.getClaim(AuthUserInfo.KEY_LEVEL)
        val level = if (levelClaim.isNull) {
            null
        }else levelClaim.asInt()

        val roleClaim = payload.getClaim(AuthUserInfo.KEY_ROLE)
        val role = if (roleClaim.isNull) {
            null
        }else roleClaim.asString().split(",")

        val authInfo = AuthUserInfo(uId, level, role)
        val result = hasPermission(call, action, subject, authInfo)
        authInfo.isAllow = result

        //log.info("set authInfo in JwtHelper.isAuthorized")
        call.authInfo =  authInfo
        return result
    }

    /**
     * 是否有权限
     * 用于ApplicationCall的intercept时检查该操作是否有权限
     * @return 具备操作权限返回true，否则返回false; 返回null时表示TBD待决定(适合于数据需要owner时的情况)
     * */
    abstract fun hasPermission(call: ApplicationCall, action: Action?, subject: String?, authUserInfo: AuthUserInfo): Boolean?
}

class TestJwtHelper: UserInfoJwtHelper("test secret key","test issuer") {
    override fun isAuthentic(uId: String,payload: Payload) = true

    override fun hasPermission(
        call: ApplicationCall,
        action: Action?,
        subject: String?,
        authUserInfo: AuthUserInfo
    ) = true
}

class Test2JwtHelper: AbstractJwtHelper("test secret key","test issuer") {
    override fun validate(payload: Payload)= true

    override fun isAuthorized(call: ApplicationCall, action: Action?, subject: String?): Boolean? {
        return true
    }
}