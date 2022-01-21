package com.github.rwsbillyang.ktorExt

import com.alipay.sofa.rpc.common.RpcConstants


object MyRpcConfig {
    //const val SOFA_REGISTRY_PROTOCOL = RpcConstants.REGISTRY_PROTOCOL_ZK
    var SOFA_REGISTRY_PROTOCOL = RpcConstants.REGISTRY_PROTOCOL_LOCAL

    var SOFA_REGISTRY_ADDRESS = "127.0.0.1:2181"

    //var SOFALocalRegistry = System.getProperty("user.home") + File.separator + "localRegistry"
    var SOFALocalRegistry = "./localRegistry" //改为当前目录下, 避免prod和dev 共用产生数据交叉行为


    /**
     * 默认端口12200
     */
    var SOFA_SERVER_PORT = 12200
}