package com.github.rwsbillyang.ktorExt


import com.alipay.sofa.rpc.common.RpcConstants
import com.alipay.sofa.rpc.config.RegistryConfig
import com.alipay.sofa.rpc.config.ServerConfig
import com.alipay.sofa.rpc.context.RpcInternalContext
import com.alipay.sofa.rpc.context.RpcInvokeContext
import com.alipay.sofa.rpc.context.RpcRuntimeContext
import com.github.rwsbillyang.ktorExt.MyRpcConfig.SOFALocalRegistry
import io.ktor.application.*

import org.koin.core.KoinComponent


import org.slf4j.LoggerFactory


/**
 * 服务发布过程涉及到三个类 RegistryConfig ，ServerConfig ，ProviderConfig
 * RegistryConfig 表示注册中心。如声明服务注册中心的地址和端口是127.0.0.1:2181，协议是 Zookeeper。
 * ServerConfig 表示服务运行容器。如声明一个使用8803端口和 bolt 协议的 server 。
 * ProviderConfig 表示服务发布。如声明服务的接口，实现和该服务运行的 server 。 最终通过 export 方法将这个服务发布出去。
 *
 * https://www.sofastack.tech/projects/sofa-rpc/programing-rpc/
 * */
abstract class SofaRpc(application: Application): LifeCycle(application), KoinComponent{
    private val log = LoggerFactory.getLogger("SofaRpcConfig")

    companion object {
        @Volatile
        var closed = false

        val registryConfig: RegistryConfig = RegistryConfig()
            .setProtocol(MyRpcConfig.SOFA_REGISTRY_PROTOCOL)
            .run {
                if(MyRpcConfig.SOFA_REGISTRY_PROTOCOL == RpcConstants.REGISTRY_PROTOCOL_LOCAL)
                {
                    //用法参见测试文件：LocalRegistryTest
                    setFile(SOFALocalRegistry)
                }else{
                    setAddress(MyRpcConfig.SOFA_REGISTRY_ADDRESS)
                }
            }

        val serverConfig: ServerConfig = ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT) // 设置一个协议，默认bolt
            .setSerialization(RpcConstants.SERIALIZE_HESSIAN2)
            //.setPort(MyRpcConfig.SOFA_SERVER_PORT + 10) // 设置一个端口，默认12200
            .setAdaptivePort(true)//当端口被占用，自动+1进行适应,默认为false
            .setDaemon(true) // 非守护线程,是否hold住端口，true的话随主线程退出而退出，false的话则要主动退出
    }

    init {
        //Koin注入创建本实例，也就是执行到此init处，才注册本KoinApplicationOnStarted事件处理器
        onStarted{
            publicRpcService()
            injectRpcClient()
        }
        onStopping{
            rpcExit()
        }
        subscribeEvent()
    }

    abstract fun publicRpcService()
    abstract fun injectRpcClient()

    private fun rpcExit(){
        if(!closed){ //确保一个app实例中只调用一次
            closed = true
            log.info("###to clean rpc...###")
            RpcRuntimeContext.destroy()
            RpcInternalContext.removeContext()
            RpcInvokeContext.removeContext()
        }else{
            log.warn("already onExit, do nothing")
        }
    }
}

