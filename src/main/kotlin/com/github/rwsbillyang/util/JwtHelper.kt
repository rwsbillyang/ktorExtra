/*
 * Copyright © 2019 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2019-12-26 12:04
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
//import io.ktor.application.ApplicationCall
//import io.ktor.auth.authentication
//import io.ktor.auth.jwt.JWTPrincipal
//import io.ktor.request.uri
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 * */
enum class Role { Guest, User, Admin ,Root }

/**
 * 暂只有读写两样权限
 * */
enum class Operation { READ, WRITE }

/**
 * 扩展成员 用户Id，从token中提取
 * */
//val ApplicationCall.uId: String
//    get() = this.authentication.principal<JWTPrincipal>()!!.payload.getClaim(JwtHelper.KeyUID).asString()

/**
 * @param secretKey 秘钥，如RSA秘钥
 * @param issuer 填写自己的域名
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
class JwtHelper(
    secretKey: String,
    private val issuer: String,
    private val audience: String = "webapp",
    private val subject: String = "server",
    private val expireDays: Int = 90
) {

    companion object {
        const val KeyUID = "uId"
        const val KeyLevel = "level"
    }

    private val log = LoggerFactory.getLogger("JwtHelper")

    private val algorithm: Algorithm = Algorithm.HMAC256(secretKey)

    /**
     * @param jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
     * */
    fun createToken(jti: String, claims: HashMap<String, String>? = null): String? {
        val now = System.currentTimeMillis()
        return try {
            val t = JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withAudience(audience)
                .withIssuedAt(Date(now))
                .withExpiresAt(Date(now + expireDays * 24 * 3600000))
                .withJWTId(jti)

            claims?.forEach { t.withClaim(it.key, it.value) }
            t.sign(algorithm)
        } catch (exception: JWTCreationException) {
            //Invalid Signing configuration / Couldn't convert Claims.
            null
        }
    }

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
    fun jwtBuilder(jti: String): JWTCreator.Builder {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expireDays * 24 * 3600000L))
            .withJWTId(jti)
    }

    /**
     * 使用jwtBuilder之后，再withClaim之后，最后生成token
     * <code>
     *     val jti = RandomStringUtils.randomAlphanumeric(8)
     *     val uId = user._id!!.toHexString()
     *     val token = jwtHelper
     *     .createToken(
     *      jwtHelper.jwtBuilder(jti)
     *      .withClaim(JwtHelper.KeyUID, uId)
     *      .withClaim(JwtHelper.KeyLevel, user.level)
     *      )
     * </code>
     * */
    fun createToken(builder: JWTCreator.Builder) = builder.sign(algorithm)

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


//    /**
//     * 是否有权限
//     * */
//    fun hasPermission(call: ApplicationCall, scope: Scope, operation: Operation): Boolean{
//        val payload = call.authentication.principal<JWTPrincipal>()?.payload
//        if(payload == null){
//            log.warn("payload is null")
//            return false
//        }
//
//        val levelClaim = payload.getClaim(KeyLevel)
//        if(levelClaim.isNull){
//            log.warn("levelClaim is null")
//            return false
//        }
//        val level = levelClaim.asInt()
//
//        val flag = when(scope){
//            Scope.Sample -> {
//                when(operation){
//                    Operation.READ -> true
//                    Operation.WRITE -> level <= 10
//                }
//            }
//            Scope.Collector ->{
//                when(operation){
//                    Operation.READ -> level <= 10
//                    Operation.WRITE -> level == 0
//                }
//            }
//        }
//        if(!flag){
//            val uIdClaim = payload.getClaim(KeyUID)
//            if(uIdClaim.isNull)
//            {
//                log.warn("uIdClaim is null")
//            }else{
//                log.warn("no permission: ${call.request.uri}, uId = ${uIdClaim.asString()}")
//            }
//
//        }
//        return flag
//    }
}


