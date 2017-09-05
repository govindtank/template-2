package com.xixi.fruitshop.android.module.home

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.qihoo360.replugin.RePlugin
import com.qihoo360.replugin.model.PluginInfo
import com.qihoo360.replugin.utils.FileUtils
import com.xixi.library.android.base.FSBaseFragment
import com.xixi.library.android.util.FSToastUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class SettingFragment : FSBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val textView: TextView = TextView(activity)
        textView.text = "setting"
        textView.setTextColor(resources.getColor(R.color.fs_orange))
        textView.setBackgroundColor(Color.DKGRAY)
        textView.setOnClickListener {
            /*FSRouteManager.goToFragment(activity, "com.xixi.fruitshop.android.module.setting.SettingFragment") {
                FSLogUtil.w("krmao", it.toString())
            }*/
            val pd = ProgressDialog.show(activity, "Installing...", "Please wait...", true, true)
            // FIXME: 仅用于安装流程演示 2017/7/24
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                simulateInstallExternalPlugin()
                pd.dismiss()
            }, 1000)
        }
        return textView
    }

    override fun onStart() {
        super.onStart()
        Log.w("krmao", "SettingFragment:onStart");
    }

    override fun onStop() {
        super.onStop()
        Log.w("krmao", "SettingFragment:onStop");
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w("krmao", "SettingFragment:onDestroy");
    }

    /**
     * 模拟安装或升级（覆盖安装）外置插件
     * 注意：为方便演示，外置插件临时放置到Host的assets/external目录下，具体说明见README
     */
    private fun simulateInstallExternalPlugin() {
        val demo3Apk = "setting.apk"
        val demo3apkPath = "external" + File.separator + demo3Apk

        // 文件是否已经存在？直接删除重来
        val pluginFilePath = context.filesDir.absolutePath + File.separator + demo3Apk
        val pluginFile = File(pluginFilePath)
        if (pluginFile.exists()) {
            FileUtils.deleteQuietly(pluginFile)
        }

        // 开始复制
        copyAssetsFileToAppFiles(demo3apkPath, demo3Apk)
        var info: PluginInfo? = null
        if (pluginFile.exists()) {
            info = RePlugin.install(pluginFilePath)
        }

        if (info != null) {
            RePlugin.startActivity(activity, RePlugin.createIntent(info.name, "com.qihoo360.replugin.sample.demo3.MainActivity"))
        } else {
            FSToastUtil.show("install external plugin failed")
        }
    }

    /**
     * 从assets目录中复制某文件内容
     * @param  assetFileName assets目录下的Apk源文件路径
     * @param  newFileName 复制到/data/data/package_name/files/目录下文件名
     */
    private fun copyAssetsFileToAppFiles(assetFileName: String, newFileName: String) {
        var inputStream: InputStream? = null
        var fos: FileOutputStream? = null
        val buffsize = 1024

        try {
            inputStream = context.assets.open(assetFileName)
            fos = context.openFileOutput(newFileName, Context.MODE_PRIVATE)
            var buffer = ByteArray(buffsize)
            val byteCount = inputStream.read(buffer)
            while (byteCount != -1) {
                fos.write(buffer, 0, byteCount)
                buffer = ByteArray(buffsize)
            }
            fos.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
                fos?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}
