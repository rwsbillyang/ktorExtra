package com.github.rwsbillyang.ktorExt

import io.ktor.application.*
import org.koin.core.KoinApplication
import org.koin.ktor.ext.KoinApplicationStarted

/**
 * convenience for application subscription
 * */
open class LifeCycle(val application: Application) {

    /**
     * 指定启动后的执行代码块
     * */
    fun onStarted(block: (KoinApplication) -> Unit) {
        onStartedHandler = block
    }
    /**
     * 指定关闭时的执行代码块
     * */
    fun onStopping(block: (Application) -> Unit) {
        onStoppingHandler = block
    }

    private var onStartedHandler: ((KoinApplication) -> Unit)? = null
    private var onStoppingHandler: ((Application) -> Unit)? = null
    private var onStoppedHandler: ((Application) -> Unit)? = null
    private var hasSubscription = false

    /**
     * 指定完启动后和关闭时的代码块，需要调用subscribeEvent()进行订阅
     * */
    fun subscribeEvent() {
        val monitor = application.environment.monitor

        //https://start.insert-koin.io/#/getting-started/koin-for-ktor
        onStartedHandler?.let {
            //订阅启动后的执行动作
            monitor.subscribe(KoinApplicationStarted, it)
            hasSubscription = true
        }

        onStoppingHandler?.let {
            //订阅停止时的执行动作
            monitor.subscribe(ApplicationStopping, it)
            hasSubscription = true
        }

        if (hasSubscription) {
            //停止后取消订阅的动作
            onStoppedHandler = {
                onStartedHandler?.let {
                    //取消订阅
                    monitor.unsubscribe(KoinApplicationStarted, it)
                }
                onStoppingHandler?.let {
                    //取消订阅
                    monitor.subscribe(ApplicationStopping, it)
                }
                if (onStoppedHandler != null) {
                    monitor.unsubscribe(ApplicationStopped, onStoppedHandler!!)
                }
            }

            if (onStoppedHandler != null) {
                //订阅停止后的动作
                monitor.subscribe(ApplicationStopped, onStoppedHandler!!)
            }
        }
    }
}