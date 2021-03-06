package com.smart.library.bundle.model

import com.smart.library.base.CXBaseApplication
import com.smart.library.bundle.strategy.CXHybirdUpdateStrategy

data class CXHybirdModuleConfigModel(
    var moduleName: String = "",
    var moduleVersion: String = "", //只分当前版本与线上最新版本
    var moduleDebug: Boolean = CXBaseApplication.DEBUG, //只下发到测试机
    var moduleUpdateStrategy: CXHybirdUpdateStrategy = CXHybirdUpdateStrategy.ONLINE,
    var moduleMainUrl: String = "",
    var moduleConfigUrl: String = "",
    var moduleDownloadUrl: String = "",
    var moduleZipMd5: String = "",
    var moduleFilesMd5: HashMap<String, String> = HashMap<String, String>()
) {
    override fun equals(other: Any?): Boolean = other is CXHybirdModuleConfigModel && moduleVersion == other.moduleVersion && moduleName == other.moduleName
    override fun hashCode(): Int {
        var result = moduleName.hashCode()
        result = 31 * result + moduleVersion.hashCode()
        result = 31 * result + moduleDebug.hashCode()
        result = 31 * result + moduleUpdateStrategy.hashCode()
        result = 31 * result + moduleMainUrl.hashCode()
        result = 31 * result + moduleConfigUrl.hashCode()
        result = 31 * result + moduleDownloadUrl.hashCode()
        result = 31 * result + moduleZipMd5.hashCode()
        result = 31 * result + moduleFilesMd5.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        val invalidConfigModel: CXHybirdModuleConfigModel = CXHybirdModuleConfigModel()
    }
}
