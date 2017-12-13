package com.smart.library.bundle

import android.net.Uri
import android.text.TextUtils
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.smart.library.base.HKBaseApplication
import com.smart.library.util.*
import com.smart.library.util.hybird.HKHybirdBridge
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipException


/**
 * 全责范围
 *
 * 1: 负责对本模块各版本的健康体检，自动删除无效版本，删除后自动还原到上一个最新版本，如果没有则还原到原始安装包版本
 * 2: 不负责下载更新升级等操作，这些操作放在 HKHybirdUpdateManager
 */
@Suppress("MemberVisibilityCanPrivate", "unused", "UNCHECKED_CAST", "PrivatePropertyName")
class HKHybirdModuleManager(val moduleFullName: String) {

    private val TAG = HKHybirdModuleManager::class.java.simpleName + ":" + moduleFullName
    private val KEY_CONFIGURATION = "KEY-HYBIRD-CONFIGURATION-$moduleFullName"


    private val rootDir = File(HKHybirdManager.LOCAL_ROOT_DIR, moduleFullName)

    fun getZipFile(configuration: HKHybirdModuleConfiguration?): File = File(rootDir, "${configuration?.moduleName}-${configuration?.moduleVersion}${HKHybirdManager.BUNDLE_SUFFIX}")
    fun getUnzipDir(configuration: HKHybirdModuleConfiguration?): File? = File(rootDir, configuration?.moduleVersion)

    var onLineMode = false

    /**
     * 健康体检，检查模块完整性
     * 每次打开浏览器时执行
     *
     * 由于 initLocalConfiguration 是同步的，所以在任何地方都可以调用  checkHealth 而不会重复校验,是序列化的
     */
    @Synchronized
    fun checkHealth(callback: ((localUnzipDir: File?, configuration: HKHybirdModuleConfiguration?) -> Unit)? = null) {
        HKLogUtil.w(TAG, "checkHealth >>>>>>>>>>===============>>>>>>>>>>")
        val start = System.currentTimeMillis()
        if (latestValidConfiguration == null || !isLocalFilesValid(latestValidConfiguration)) {

            Observable.fromCallable {
                initLocalConfiguration()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    callback?.invoke(getUnzipDir(latestValidConfiguration), latestValidConfiguration)
                    HKLogUtil.e(TAG, "checkHealth complete , 耗时: ${System.currentTimeMillis() - start}ms")
                    HKLogUtil.w(TAG, "checkHealth <<<<<<<<<<===============<<<<<<<<<<")
                }
            HKLogUtil.w(TAG, "checkHealth async-progressing , 耗时: ${System.currentTimeMillis() - start}ms")
        } else {
            callback?.invoke(getUnzipDir(latestValidConfiguration), latestValidConfiguration)
            HKLogUtil.e(TAG, "checkHealth complete , 耗时: ${System.currentTimeMillis() - start}ms")
            HKLogUtil.w(TAG, "checkHealth <<<<<<<<<<===============<<<<<<<<<<")
        }
    }

    /**
     * 安全的删除所有大于 @param latestConfiguration 的版本的 临时配置信息以及与之相关的本地文件，包括压缩包和解压文件夹以及sharedPreference里面的配置信息
     * 注意这里会重置 onLineMode 的值
     */
    @Synchronized //同步，确保所有其他操作在此期间都是等待状态，最好这里是原子性的, @Synchronized注解锁住的是对该类对象的访问操作
    fun completeRemoveAllGTLatestConfigSafely(latestConfiguration: HKHybirdModuleConfiguration?) {
        HKLogUtil.e(TAG, "completeRemoveAllGTLatestConfigSafely start")
        val start = System.currentTimeMillis()
        if (latestConfiguration != null) {
            val latestVersion = latestConfiguration.moduleVersion.toFloatOrNull()
            if (latestVersion != null) {
                val oldOnLineMode = onLineMode
                onLineMode = true
                val configurationList = getConfigurationList()

                val iterate = configurationList.listIterator()
                while (iterate.hasNext()) {
                    val tmpConfiguration = iterate.next()
                    val zipFile = getZipFile(tmpConfiguration)
                    val unzipDir = getUnzipDir(tmpConfiguration)
                    val tmpVersion = tmpConfiguration.moduleVersion.toFloatOrNull()
                    if (tmpVersion == null || tmpVersion > latestVersion) {
                        iterate.remove()                            //删除在list中的位置
                        HKFileUtil.deleteFile(zipFile)              //删除 zip
                        HKFileUtil.deleteDirectory(unzipDir)        //删除 unzipDir
                    }
                }
                saveConfiguration(configurationList)                //彻底删除配置信息，至此已经删除了所有与本版本相关的信息
                initLocalConfiguration()                            //重置 latestValidConfiguration ，并且list为空自动重新拷贝原始安装包

                //还原 onLineMode
                onLineMode = oldOnLineMode
            }
        }
        HKLogUtil.e(TAG, "completeRemoveAllGTLatestConfigSafely   end  耗时:${System.currentTimeMillis() - start}ms")
    }
    //==============================================================================================
    // updater
    //==============================================================================================

    private val updateManager: HKHybirdUpdateManager = HKHybirdUpdateManager(this)

    fun checkUpdate() {
        updateManager.checkUpdate()
    }

    fun setDownloader(downloader: (downloadUrl: String, file: File?, callback: (File?) -> Unit?) -> Unit?) {
        updateManager.downloader = downloader
    }

    fun setConfiger(configer: (configUrl: String, callback: (HKHybirdModuleConfiguration?) -> Unit?) -> Unit?) {
        updateManager.configer = configer
    }

    //==============================================================================================
    //配置信息
    //==============================================================================================

    /**
     * zip准备好后保存到本地, verify Local Zip==false 时从本地删除
     */
    //TODO 新版本文件破坏，读取原始版本，但是原始版本本地存在，则校验成功？
    //TODO 本地文件存在，只是配置丢失，注意此时情况
    var latestValidConfiguration: HKHybirdModuleConfiguration? = null
        set(value) {
            field = value
            setRequestIntercept(value)
        }

    //配置信息存在 sharedPreference
    //赋值最新版本信息
    //做到可以回滚
    //只校验有配置信息的，校验失败可以删除，但是没有校验信息的本地文件夹不要删除，下次下载好配置信息好先检测本地zip包是否已经有了，避免重复下载，如果有解压后的文件夹但是没有zip包，不删除，可以直接
    @Synchronized
    private fun initLocalConfiguration() {
        HKLogUtil.e(TAG, "initLocalConfiguration start")
        val start = System.currentTimeMillis()
        val configurationList = getConfigurationList()//读取配置信息
        HKLogUtil.d(TAG, "sorted list : ${configurationList.map { it.moduleVersion }}")
        if (configurationList.isNotEmpty()) {
            HKLogUtil.d(TAG, "configurationList not empty")

            //直到找到有效的版本，如果全部无效，则在后续步骤中重新解压原始版本
            val iterate = configurationList.listIterator()
            while (iterate.hasNext()) {
                if (!isLocalFilesValid(iterate.next())) {
                    iterate.remove() // mind ConcurrentModificationException
                }
            }
        }

        //删除无效的配置信息,删除后会重新判断空,如果是空会获取原始安装包
        if (configurationList.isEmpty()) {
            HKLogUtil.d(TAG, "configurationList is empty")
            getConfigurationFromAssets()?.let { configurationList.add(it) } //如果为空则添加原始配置信息
        }
        //如果有删除的或则新加的原始配置信息，则需要重新保存下
        saveConfiguration(configurationList)

        latestValidConfiguration = null
        configurationList.firstOrNull()?.let { latestValidConfiguration = it }
        HKLogUtil.e(TAG, "initLocalConfiguration reset latestValidConfiguration:$latestValidConfiguration")
        HKLogUtil.e(TAG, "initLocalConfiguration   end  耗时:${System.currentTimeMillis() - start}ms")
        return
    }

    @Synchronized
    private fun saveConfiguration(configurationList: MutableList<HKHybirdModuleConfiguration>) {
        HKPreferencesUtil.putList(KEY_CONFIGURATION, configurationList)
        HKLogUtil.w("save sorted list : ${configurationList.map { it.moduleVersion }}")
    }

    // 保存/替换配置信息
    @Synchronized
    fun saveConfiguration(configuration: HKHybirdModuleConfiguration) {
        val configurationList = getConfigurationList()
        if (configurationList.contains(configuration))
            configurationList.remove(configuration)
        configurationList.add(0, configuration)
        saveConfiguration(configurationList)
    }

    @Synchronized
    private fun getConfigurationList(): MutableList<HKHybirdModuleConfiguration> = HKPreferencesUtil.getList(KEY_CONFIGURATION, HKHybirdModuleConfiguration::class.java).filter { it.moduleVersion.toFloatOrNull() != null }.sortedByDescending { it.moduleVersion.toFloatOrNull() ?: -1f }.toMutableList()

//==============================================================================================
//检验文件
//==============================================================================================

    /**
     * 校验配置信息
     *
     * 规则
     * 1: 文件夹校验成功则成功，zip包的校验不影响结果，如果zip包校验失败则删除zip包
     */
    fun isLocalFilesValid(configuration: HKHybirdModuleConfiguration?): Boolean {
        HKLogUtil.w(TAG, "verify start ${configuration?.moduleName}:${configuration?.moduleVersion}")
        val success: Boolean
        val start = System.currentTimeMillis()
        val zipFile = getZipFile(configuration)
        val unzipDir = getUnzipDir(configuration)
        if (!verifyLocalFiles(unzipDir, configuration?.moduleFilesMd5)) {
            if (!verifyZip(zipFile, configuration?.moduleZipMd5)) {
                success = false
            } else {
                success = unzipToLocal(zipFile, unzipDir)//解压后的文件夹校验失败，但是zip包校验成功，则重新解压即可
            }
        } else {
            success = true
        }
        HKLogUtil.w(TAG, "verify   end ${configuration?.moduleName}:${configuration?.moduleVersion} , verify ${if (success) "success" else "failure"} , 耗时: ${System.currentTimeMillis() - start}ms")
        return success
    }

    @Synchronized
    private fun getConfigurationFromAssets(): HKHybirdModuleConfiguration? {
        val start = System.currentTimeMillis()
        var configuration: HKHybirdModuleConfiguration? = null
        try {
            configuration = HKJsonUtil.fromJson(HKFileUtil.readTextFromFile(HKBaseApplication.INSTANCE.assets.open("${HKHybirdManager.ASSETS_DIR_NAME}/$moduleFullName${HKHybirdManager.CONFIG_SUFFIX}")), HKHybirdModuleConfiguration::class.java)
            HKLogUtil.d(TAG, "getConfigurationFromAssets success , 耗时: ${System.currentTimeMillis() - start}ms \nconfiguration:$configuration")

            val zipFile = getZipFile(configuration)
            val unzipDir = getUnzipDir(configuration)
            if (copyPrimaryZipFromAssets(configuration)) {
                unzipToLocal(zipFile, unzipDir)
            }
        } catch (exception: FileNotFoundException) {
            HKLogUtil.e(TAG, "getConfigurationFromAssets failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        } catch (exception: IOException) {
            HKLogUtil.e(TAG, "getConfigurationFromAssets failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        }
        return configuration
    }

    @Synchronized
    private fun copyPrimaryZipFromAssets(primaryConfiguration: HKHybirdModuleConfiguration?): Boolean {
        var success = false
        val start = System.currentTimeMillis()
        try {
            val zipFile = getZipFile(primaryConfiguration)
            val unzipDir = getUnzipDir(primaryConfiguration)

            HKFileUtil.deleteFile(zipFile)
            HKFileUtil.deleteDirectory(unzipDir)
            HKFileUtil.copy(HKBaseApplication.INSTANCE.assets.open("${HKHybirdManager.ASSETS_DIR_NAME}/$moduleFullName-${primaryConfiguration?.moduleVersion}${HKHybirdManager.BUNDLE_SUFFIX}"), zipFile)
            success = true
            HKLogUtil.d(TAG, "copyPrimaryZipFromAssets success ,localZipFile.exists?${zipFile.exists()} , 耗时: ${System.currentTimeMillis() - start}ms")
        } catch (exception: FileNotFoundException) {
            HKLogUtil.e(TAG, "copyPrimaryZipFromAssets failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        } catch (exception: IOException) {
            HKLogUtil.e(TAG, "copyPrimaryZipFromAssets failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        }
        return success
    }

    @Synchronized
    private fun unzipToLocal(zipFile: File, unZipDir: File?): Boolean {
        var success = false
        val start = System.currentTimeMillis()
        HKFileUtil.deleteDirectory(unZipDir)
        try {
            HKZipUtil.unzip(zipFile, unZipDir)
            success = true
            HKLogUtil.d(TAG, "unzipToLocal success , 耗时: ${System.currentTimeMillis() - start}ms")
        } catch (exception: FileNotFoundException) {
            HKLogUtil.e(TAG, "unzipToLocal failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        } catch (exception: IOException) {
            HKLogUtil.e(TAG, "unzipToLocal failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        } catch (exception: ZipException) {
            HKLogUtil.e(TAG, "unzipToLocal failure , 耗时: ${System.currentTimeMillis() - start}ms", exception)
        }
        return success
    }

    /**
     * 校验失败删除压缩包
     */
    private fun verifyZip(zipFile: File?, moduleZipMd5: String?): Boolean {
        val start = System.currentTimeMillis()
        var success = false
        if (zipFile != null && !TextUtils.isEmpty(moduleZipMd5)) {
            val zipFileExists = zipFile.exists()
            val zipFileMd5 = HKChecksumUtil.genMD5Checksum(zipFile)
            val isZipFileMd5Valid = zipFileMd5 == moduleZipMd5
            success = zipFileExists && isZipFileMd5Valid
            HKLogUtil.d(TAG, "verifyZip (${zipFile.name}) : ${if (success) "success" else "failure"}, zip文件是否存在:$zipFileExists, MD5是否正确:$isZipFileMd5Valid, 耗时: ${System.currentTimeMillis() - start}ms")
        } else {
            HKLogUtil.e(TAG, "verifyZip (${zipFile?.name}) : ${if (success) "success" else "failure"}, zipFile or moduleZipMd5 is null, 耗时: ${System.currentTimeMillis() - start}ms")
        }
        if (!success) HKFileUtil.deleteFile(zipFile)
        return success
    }

    /**
     * 校验失败删除文件夹
     */
    private fun verifyLocalFiles(unZipDir: File?, moduleFilesMd5: HashMap<String, String>?): Boolean {
        val start = System.currentTimeMillis()
        var success = false
        if (unZipDir != null && moduleFilesMd5 != null) {
            val localUnzipDirExists = unZipDir.exists()
            val localIndexExists = File(unZipDir, "index.shtml").exists()
            var invalidFilesNum = 0
            if (localIndexExists && localIndexExists) {
                HKFileUtil.getFileList(unZipDir).forEach {
                    val fileMd5 = HKChecksumUtil.genMD5Checksum(it)
                    val remotePath = it.absolutePath.replace(unZipDir.absolutePath, "")
                    val rightMd5 = moduleFilesMd5[remotePath]
                    val isFileMd5Valid = fileMd5 == rightMd5
                    if (!isFileMd5Valid)
                        invalidFilesNum++
                    HKLogUtil.d(TAG, "verifyLocalFiles : isFileMd5Valid:$isFileMd5Valid , fileMd5:$fileMd5 , rightMd5:$rightMd5 , localPath:${it.path} ,remotePath:$remotePath")
                }
                success = invalidFilesNum == 0 && localUnzipDirExists && localIndexExists
            }
            HKLogUtil.w(TAG, "verifyLocalFiles(${unZipDir.name}) : ${if (success) "success" else "failure"}, invalidFilesNum:$invalidFilesNum, localUnzipDirExists:$localUnzipDirExists, localIndexExists:$localIndexExists, 耗时: ${System.currentTimeMillis() - start}ms")
        } else {
            HKLogUtil.e(TAG, "verifyLocalFiles(${unZipDir?.name}) : ${if (success) "success" else "failure"}, unZipDir or moduleFilesMd5 is null, 耗时: ${System.currentTimeMillis() - start}ms")
        }
        if (!success) HKFileUtil.deleteDirectory(unZipDir)
        return success
    }

//==============================================================================================
// intercept
//==============================================================================================

    private fun setRequestIntercept(configuration: HKHybirdModuleConfiguration?) {
        HKLogUtil.w(TAG, "setRequestIntercept start")
        HKLogUtil.w(TAG, "configuration : $configuration")
        val interceptScriptUrl = configuration?.moduleScriptUrl?.get(HKHybirdManager.EVN) ?: return
        val interceptMainUrl = configuration.moduleMainUrl.get(HKHybirdManager.EVN) ?: return
        val unzipDir = getUnzipDir(configuration)

        HKHybirdBridge.removeScheme(interceptMainUrl)
        HKHybirdBridge.removeRequest(interceptMainUrl)
        HKHybirdBridge.removeRequest(interceptScriptUrl)

        /**
         * webView.loadUrl 不会触发此回调,放到 HKHybirdBridge.addRequest(interceptMainUrl) 里面处理
         * http://www.jianshu.com/p/3474cb8096da
         */
        /*HKLogUtil.w(TAG, "addScheme interceptMainUrl : $interceptMainUrl")
        HKHybirdBridge.addScheme(interceptMainUrl) { _: WebView?, url: String? ->
            HKLogUtil.w(TAG, "拦截到 scheme : $url")
            checkUpdate()
            false
        }*/

        //html
        HKLogUtil.w(TAG, "addRequest interceptMainUrl : $interceptMainUrl")
        HKHybirdBridge.addRequest(interceptMainUrl) { _: WebView?, url: String? ->
            HKLogUtil.w(TAG, "拦截到 request : $url")
            checkUpdate()
            var resourceResponse: WebResourceResponse? = null
            if (!TextUtils.isEmpty(url)) {
                val requestUrl = Uri.parse(url)
                val scheme = requestUrl?.scheme?.trim()
                if (requestUrl != null && ("http".equals(scheme, true) || "https".equals(scheme, true))) {
                    val requestUrlString = requestUrl.toString()
                    if (interceptMainUrl == requestUrlString) {
                        val mimeType = "text/html"
                        val localPath = unzipDir?.absolutePath + "/" + requestUrlString.substringBefore("#", requestUrlString).split("/").last()
                        val localFileExists = File(localPath).exists()
                        HKLogUtil.d(TAG, "**** do intercept request ? $localFileExists **** [originPath: " + requestUrl.toString() + "], [localPath: $localPath]")
                        if (localFileExists) {
                            try {
                                resourceResponse = WebResourceResponse(mimeType, "UTF-8", FileInputStream(localPath))
                            } catch (e: Exception) {
                                HKLogUtil.e(TAG, "**** do intercept request ? false **** ", e)
                            }
                        } else {
                            HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], [localPath(exist?$localFileExists): $localPath]")
                        }
                    } else {
                        HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], interceptMainUrl != requestUrlString")
                    }
                } else {
                    HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], requestUrl ==null || scheme != http && scheme != https")
                }
            } else {
                HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], url is empty")
            }
            resourceResponse
        }

        HKLogUtil.w(TAG, "interceptScriptUrl : $interceptScriptUrl")
        //css,js,image
        HKHybirdBridge.addRequest(interceptScriptUrl) { _: WebView?, url: String? ->
            var resourceResponse: WebResourceResponse? = null
            if (!onLineMode) {
                if (!TextUtils.isEmpty(url)) {
                    val requestUrl = Uri.parse(url)
                    val scheme = requestUrl?.scheme?.trim()
                    if (requestUrl != null && ("http".equals(scheme, true) || "https".equals(scheme, true))) {
                        if (url!!.contains(interceptScriptUrl, true)) {

                            val requestUrlString = requestUrl.toString()

                            val mimeType: String = when {
                                requestUrlString.contains(".css") -> "text/css"
                                requestUrlString.contains(".png") -> "image/png"
                                requestUrlString.contains(".js") -> "application/x-javascript"
                                requestUrlString.contains(".woff") -> "application/x-font-woff"
                                requestUrlString.contains(".html") -> "text/html"
                                requestUrlString.contains(".shtml") -> "text/html"
                                else -> "text/html"
                            }
                            val tmpPath = requestUrlString
                                .replace(interceptScriptUrl, "")
                                .replace("https://", "")
                                .replace("http://", "")
                            val localPath = unzipDir?.absolutePath + tmpPath

                            val localFileExists = File(localPath).exists()
                            HKLogUtil.e(TAG, "**** do intercept request ? $localFileExists **** [originPath: $requestUrlString], [localPath: $localPath]")
                            if (localFileExists) {
                                try {
                                    resourceResponse = WebResourceResponse(mimeType, "UTF-8", FileInputStream(localPath))
                                } catch (e: Exception) {
                                    HKLogUtil.e(TAG, "**** do intercept request ? false **** ", e)
                                }
                            } else {
                                HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], [localPath(exist?$localFileExists): $localPath]")
                            }
                        } else {
                            HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], interceptMainUrl != requestUrlString")
                        }
                    } else {
                        HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], requestUrl ==null || scheme != http && scheme != https")
                    }
                } else {
                    HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], url is empty")
                }
            } else {
                HKLogUtil.e(TAG, "**** do intercept request ? false **** [originPath: $url], [interceptHost: $interceptScriptUrl], onLineMode now ...")
            }
            resourceResponse
        }
        HKLogUtil.w("setRequestIntercept end")
    }
}
