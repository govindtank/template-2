package com.xixi.fruitshop.android.library

import android.content.Context
import android.content.res.Configuration
import com.meituan.android.walle.WalleChannelReader
import com.qihoo360.replugin.RePlugin
import com.xixi.library.android.base.FSBaseApplication

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
        RePlugin.App.attachBaseContext(this);
    }

    override fun onLowMemory()
    {
        super.onLowMemory();

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onLowMemory();
    }

    override fun onTrimMemory( level:Int)
    {
        super.onTrimMemory(level);

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onTrimMemory(level);
    }

    override fun onConfigurationChanged( config: Configuration)
    {
        super.onConfigurationChanged(config);

        /* Not need to be called if your application's minSdkVersion > = 14 */
        RePlugin.App.onConfigurationChanged(config);
    }
}