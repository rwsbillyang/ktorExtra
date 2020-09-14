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
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import org.apache.commons.lang3.RandomStringUtils
import org.koin.core.KoinComponent
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 * */
enum class Role { Guest, User, Admin, Root }

/**
 * 暂只有读写两样权限
 * */
enum class OperationType { READ, WRITE }

fun JWTAuthenticationProvider.Configuration.config(jwtHelper: AbstractJwtHelper) {
    verifier(jwtHelper.verifier())
    this.realm = jwtHelper.realm
    validate { credential -> jwtHelper.validate(credential) }
}

/**
 * 扩展成员 用户Id，从token中提取
 * */
//val ApplicationCall.uId: String
//    get() = this.authentication.principal<JWTPrincipal>()!!.payload.getClaim(JwtHelper.KeyUID).asString()


abstract class AbstractJwtHelper(
    secretKey: String,
    val realm: String = "Server",
    private val issuer: String,
    private val audience: String = "webapp",
    private val subject: String = "server",
    private val expireDays: Int = 90
): KoinComponent
{
    protected val log = LoggerFactory.getLogger("AbstractJwtHelper")

    private val algorithm: Algorithm = Algorithm.HMAC256(secretKey)

    abstract fun validate(credential: JWTCredential): Principal?


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
     * 判断是否是真正的用户，根据用户Id判断用于是否是真正的用户
     *
     * @param uId 用户id
     * @return 真正的用户返回true，否则返回false
     *
     * Example code:
     * ```koltin
     * if(!ObjectId.isValid(uId)){ return false }
     * val user = userService.findById(uId)
     * return user != null && user.status != User.StatusDisabled
     * ```
     * */
    abstract fun isAuthentic(uId: String): Boolean

    /**
     * 判断用户是否对某个请求操作有权限
     * @return 具备操作权限返回true，否则返回false
     *
     * @param call 操作请求
     * @param operationType 操作类型
     * @param operation 操作对象
     *
     * */
    abstract fun isAuthorized(call: ApplicationCall, operationType: OperationType, operation: String): Boolean

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

/**
 * 写入了uId/level/role等信息的jwt token helper类
 *
 * @param secretKey 秘钥，如RSA秘钥
 * @param issuer 填写自己的域名
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
abstract class JwtHelper(
    secretKey: String,
    realm: String = "Server",
    issuer: String,
    audience: String = "webapp",
    subject: String = "server",
    expireDays: Int = 90
) : AbstractJwtHelper(secretKey, realm, issuer, audience, subject, expireDays) {

    companion object {
        const val KEY_UID = "uId"
        const val KEY_LEVEL = "level"
        const val KEY_ROLE = "role"
    }

    override fun validate(credential: JWTCredential): Principal? {
        val claim = credential.payload.getClaim(KEY_UID)
        if (claim.isNull) {
            log.info("no claim for ${KEY_UID} in jwtAuthentication")
            return null
        }

        val uId = claim.asString()
        if (!isAuthentic(uId)) {
            log.info("no user or user($uId) is disabled")
            return null
        }

        return JWTPrincipal(credential.payload)
    }


    fun generateToken(uId: String, level: Int?, role: List<String>?): String {
        val jti = RandomStringUtils.randomAlphanumeric(8)
        return generateToken(jti) {
            withClaim(KEY_UID, uId)
            if (level != null) withClaim(KEY_LEVEL, level.toString())
            if (!role.isNullOrEmpty()) withClaim(KEY_ROLE, role.joinToString(","))
            this
        }
    }

    override fun isAuthorized(call: ApplicationCall, operationType: OperationType, operation: String): Boolean {
        val payload = call.authentication.principal<JWTPrincipal>()?.payload
        if (payload == null) {
            log.warn("payload is null")
            return false
        }

        val uIdClaim = payload.getClaim(KEY_UID)
        if (uIdClaim.isNull) {
            log.warn("uIdClaim is null")
            return false
        }

        val uId: String = uIdClaim.asString()

        val levelClaim = payload.getClaim(KEY_LEVEL)
        val level = if (levelClaim.isNull) {
            null
        }else levelClaim.asInt()

        val roleClaim = payload.getClaim(KEY_ROLE)
        val role = if (roleClaim.isNull) {
            null
        }else roleClaim.asString().split(",")

        return hasPermission(operationType, operation, uId, level, role)
    }

    /**
     * 是否有权限
     * 用于ApplicationCall的intercet时检查该操作是否有权限
     * */
    abstract fun hasPermission(operationType: OperationType, operation: String, uId: String, level: Int?, role: List<String>?): Boolean
}


