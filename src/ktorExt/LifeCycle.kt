package ktorExt

import io.ktor.application.*

/**
 * convenience for application subscription
 * */
open class LifeCycle(val application: Application) {

    fun onStarted(block: (Application) -> Unit) {
        onStartedHandler = block
    }

    fun onStopping(block: (Application) -> Unit) {
        onStoppingHandler = block
    }

    private var onStartedHandler: ((Application) -> Unit)? = null
    private var onStoppingHandler: ((Application) -> Unit)? = null
    private var onStoppedHandler: ((Application) -> Unit)? = null
    private var hasSubscription = false

    fun subscribeEvent() {
        val monitor = application.environment.monitor

        onStartedHandler?.let {
            //订阅启动后的执行动作
            monitor.subscribe(ApplicationStarted, it)
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
                    monitor.unsubscribe(ApplicationStarted, it)
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