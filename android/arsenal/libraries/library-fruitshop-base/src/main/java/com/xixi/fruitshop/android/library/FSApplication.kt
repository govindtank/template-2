package com.xixi.fruitshop.android.library

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.meituan.android.walle.WalleChannelReader
import com.qihoo360.replugin.RePlugin
import com.qihoo360.replugin.RePluginCallbacks
import com.qihoo360.replugin.RePluginConfig
import com.qihoo360.replugin.RePluginEventCallbacks
import com.xixi.library.android.base.FSBaseApplication
import com.xixi.library.android.util.FSLogUtil


@Suppress("unused")
open class FSApplication : FSBaseApplication() {

    companion object {
        lateinit var INSTANCE: FSApplication
    }

    val channel: String by lazy { WalleChannelReader.getChannel(this) ?: "" }

    override fun onCreate() {
        super.onCreate()

        RePlugin.App.onCreate();
        INSTANCE = this
        FSInitializer.init()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base);

        val rePluginConfig = RePluginConfig()
        // 允许“插件使用宿主类”。默认为“关闭”
        rePluginConfig.isUseHostClassIfNotFound = true
        // FIXME RePlugin默认会对安装的外置插件进行签名校验，这里先关掉，避免调试时出现签名错误
        rePluginConfig.verifySign = !BuildConfig.DEBUG
        // 针对“安装失败”等情况来做进一步的事件处理
        rePluginConfig.eventCallbacks = HostEventCallbacks(this)
        // FIXME 若宿主为Release，则此处应加上您认为"合法"的插件的签名，例如，可以写上"宿主"自己的。
        // RePlugin.addCertSignature("AAAAAAAAA");
        rePluginConfig.callbacks = HostCallbacks(base)
        RePlugin.App.attachBaseContext(this);
        // FIXME 允许接收rpRunPlugin等Gradle Task，发布时请务必关掉，以免出现问题
        RePlugin.enableDebugger(base, BuildConfig.DEBUG);
    }

    override fun onLowMemory() {
        super.onLowMemory();

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onLowMemory();
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level);

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onTrimMemory(level);
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config);

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onConfigurationChanged(config);
    }

    /**
     * 宿主针对RePlugin的自定义行为
     */
    private inner class HostCallbacks constructor(context: Context) : RePluginCallbacks(context) {

        override fun onPluginNotExistsForActivity(context: Context?, plugin: String?, intent: Intent?, process: Int): Boolean {
            // FIXME 当插件"没有安装"时触发此逻辑，可打开您的"下载对话框"并开始下载。
            // FIXME 其中"intent"需传递到"对话框"内，这样可在下载完成后，打开这个插件的Activity
            if (BuildConfig.DEBUG) {
                FSLogUtil.d("onPluginNotExistsForActivity: Start download... p=$plugin; i=$intent")
            }
            return super.onPluginNotExistsForActivity(context, plugin, intent, process)
        }
    }

    class HostEventCallbacks(context: Context) : RePluginEventCallbacks(context) {

        override fun onInstallPluginFailed(path: String?, code: InstallResult?) {
            // FIXME 当插件安装失败时触发此逻辑。您可以在此处做“打点统计”，也可以针对安装失败情况做“特殊处理”
            // 大部分可以通过RePlugin.install的返回值来判断是否成功
            if (BuildConfig.DEBUG) {
                FSLogUtil.d("onInstallPluginFailed: Failed! path=$path; r=$code")
            }
            super.onInstallPluginFailed(path, code)
        }

        override fun onStartActivityCompleted(plugin: String?, activity: String?, result: Boolean) {
            // FIXME 当打开Activity成功时触发此逻辑，可在这里做一些APM、打点统计等相关工作
            super.onStartActivityCompleted(plugin, activity, result)
        }
    }
}