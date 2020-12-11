package com.github.rwsbillyang.ktorExt

import com.alipay.sofa.rpc.common.RpcConstants
import java.io.File

object MyRpcConfig {
    //const val SOFA_REGISTRY_PROTOCOL = RpcConstants.REGISTRY_PROTOCOL_ZK
    //const val SOFA_REGISTRY_ADDRESS = "127.0.0.1:2181"
    var SOFA_REGISTRY_PROTOCOL = RpcConstants.REGISTRY_PROTOCOL_LOCAL
    var SOFA_REGISTRY_ADDRESS = "local:///Users/bill/git/youke/zkdata/localRegistry"

    var SOFALocalRegistry = System.getProperty("user.home") + File.separator + "localRegistry"


    /**
     * 默认端口12200
     */
    var SOFA_SERVER_PORT = 12200
}