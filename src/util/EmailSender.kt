/*
 * Copyright © 2019 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2019-12-25 16:46
 *
 * NOTICE:
 * This software is protected by China and U.S. Copyright Law and International Treaties.
 * Unauthorized use, duplication, reverse engineering, any form of redistribution,
 * or use in part or in whole other than by prior, express, printed and signed license
 * for use is subject to civil and criminal prosecution. If you have received this file in error,
 * please notify copyright holder and destroy this and any other copies as instructed.
 */

package com.github.rwsbillyang.ktorKit.util


import org.slf4j.LoggerFactory
import java.io.UnsupportedEncodingException
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class EmailSender(private val fromMail: String,
                  val smtpHost: String? = null,
                  private val pwd: String? = null)
{
    private val log = LoggerFactory.getLogger("EmailSender")
    /**
     * 发送HTML邮件
     * */
    fun sendEmail(subject: String, body: String?, to: String, useSSL: Boolean = true): Boolean {
        if (to.isBlank() || !to.isEmail()) {
            log.error("invalid target email address")
            return false
        }

        val properties = Properties()
        if(smtpHost != null)
        {
            properties["mail.transport.protocol"] = "smtp"
            properties["mail.smtp.host"] = smtpHost
            properties["mail.smtp.auth"] = true
            properties["mail.debug"] = true
        }

        if (useSSL) {
            properties["mail.smtp.ssl.enable"]= true
            properties["mail.smtp.socketFactory.port"] = 465
            properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            properties["mail.smtp.port"] = 465

        }

        val session = if (useSSL) Session.getDefaultInstance(properties,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(fromMail, pwd)
                }
            })
        else Session.getInstance(properties)

        val title = Base64.getEncoder().encodeToString(subject.toByteArray())
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromMail, "administrator"))
                setRecipients(Message.RecipientType.TO, to)
                this.subject = "=?UTF-8?B?$title?="
                setContent(body, "text/html;charset=utf-8")
            }

            if(!useSSL && pwd != null)
            {
                val transport: Transport = session.transport
                transport.connect(fromMail, pwd)
                transport.sendMessage(message, message.allRecipients)
            }else
            {
                Transport.send(message)
            }

            true
        } catch (e: MessagingException) {
            log.error("MessagingException: ${e.message}")
            e.printStackTrace()
            false
        } catch (e2: UnsupportedEncodingException) {
            log.error("UnsupportedEncodingException: ${e2.message}")
            e2.printStackTrace()
            false
        }
    }


}