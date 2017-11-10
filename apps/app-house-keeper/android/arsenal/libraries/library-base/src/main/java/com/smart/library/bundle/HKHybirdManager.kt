package com.smart.library.bundle

import com.smart.library.util.HKLogUtil

@Suppress("MemberVisibilityCanPrivate")
object HKHybirdManager {
    private val TAG = HKHybirdManager::class.java.simpleName

    enum class Module(val fullName: String, val manager: HKHybirdModuleManager) {
        BASE("module-base", HKHybirdModuleManager("module-base")),
        MINE("module-mine", HKHybirdModuleManager("module-mine")),
    }

    fun init() {
        HKLogUtil.e(TAG, ">>>>>>>>>>>>>>>>>>>>====================>>>>>>>>>>>>>>>>>>>>")
        HKLogUtil.e(TAG, "hybird init start")
        HKLogUtil.e(TAG, ">>>>>>>>>>>>>>>>>>>>====================>>>>>>>>>>>>>>>>>>>>")

        Module.values().forEach {
            it.manager.verify()
        }

        HKLogUtil.e(TAG, "<<<<<<<<<<<<<<<<<<<<====================<<<<<<<<<<<<<<<<<<<<")
        HKLogUtil.e(TAG, "hybird init end")
        HKLogUtil.e(TAG, "<<<<<<<<<<<<<<<<<<<<====================<<<<<<<<<<<<<<<<<<<<")
    }
}
