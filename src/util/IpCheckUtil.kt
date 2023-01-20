/*
 * Copyright © 2023 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2023-01-17 22:05
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

package com.github.rwsbillyang.ktorKit.util

import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object IpCheckUtil {
    private val log = LoggerFactory.getLogger("IpCheckUtil")

    //是否来自本机IP
    fun isFromLocalIp(fromIp: String): Boolean{
        val localIps = getLocalHostAddress()
        log.info("localIps=${localIps.joinToString { " " }}") //localIp1=127.0.0.1, localIp2=10.125.22.156
        if (fromIp == "localhost"  || localIps.contains(fromIp)) {
            return true
        }
        log.warn("not from local, fromIp=$fromIp")
        return false
    }
    fun getLocalHostAddress(): Set<String> {
        val set = mutableSetOf<String>(InetAddress.getLocalHost().hostAddress)
        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces()
            var ip: InetAddress? = null
            while (allNetInterfaces.hasMoreElements()) {
                val netInterface = allNetInterfaces.nextElement() as NetworkInterface
                // if (netInterface.isLoopback || netInterface.isVirtual || !netInterface.isUp) {
                //     continue
                //} else {
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement()
                    if (ip != null && ip is Inet4Address) {
                        set.add(ip.getHostAddress())
                    }
                }
                //}
            }
        } catch (e: Exception) {
            System.err.println("fail to get ip:${e.message}")
        }

        return set
    }
}