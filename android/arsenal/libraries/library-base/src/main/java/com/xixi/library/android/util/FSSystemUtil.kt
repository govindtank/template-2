@file:Suppress("DEPRECATION")

package com.xixi.library.android.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.support.annotation.RequiresPermission
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import com.xixi.library.android.base.CXActivityLifecycleCallbacks
import com.xixi.library.android.base.CXBaseApplication
import java.lang.reflect.Method

/**
 * 所有与系统相关的方法
 */
@Suppress("unused")
object CXSystemUtil {

    val isSdCardExist: Boolean
        get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()


    val sdCardPath: String?
        get() {
            if (isSdCardExist)
                return Environment.getExternalStorageDirectory().absolutePath
            return null
        }

    /**
     * sdCardInfo[0]=总大小
     * sdCardInfo[1]=可用大小
     */
    val sdCardMemory: LongArray
        get() {
            val sdCardInfo = LongArray(2)
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == state) {
                val sdcardDir = Environment.getExternalStorageDirectory()
                val sf = StatFs(sdcardDir.path)
                val bSize = sf.blockSize.toLong()
                val bCount = sf.blockCount.toLong()
                val availBlocks = sf.availableBlocks.toLong()

                sdCardInfo[0] = bSize * bCount
                sdCardInfo[1] = bSize * availBlocks
            }
            return sdCardInfo
        }

    fun sendKeyBackEvent(context: Context) {
        if (context is Activity) {
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
            context.onKeyDown(KeyEvent.KEYCODE_BACK, keyEvent)
        }
    }

    /***
     * 隐藏软键盘
     */
    fun hideKeyboard(view: View?) {
        if (view != null) {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager.isActive)
                inputMethodManager.hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
    }

    /***
     * 隐藏软键盘
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager.isActive)
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    /***
     * 显示软键盘
     */
    fun showKeyboard(v: View) {
        val inputMethodManager = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED)
    }

    /***
     * 隐藏/显示软键盘
     */
    fun toggleKeyboard(v: View) {
        val inputMethodManager = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    val displayMetrics: DisplayMetrics
        get() = CXBaseApplication.INSTANCE.resources.displayMetrics

    val screenWidth: Int
        get() = CXBaseApplication.INSTANCE.resources.displayMetrics.widthPixels

    val screenHeight: Int
        get() = CXBaseApplication.INSTANCE.resources.displayMetrics.heightPixels


    fun getPxFromDp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
    }

    fun getPxFromPx(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, value, displayMetrics)
    }

    fun getPxFromSp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics)
    }

    val statusBarHeight2: Int
        get() {
            var statusBarHeight = 0
            val resourceId = CXBaseApplication.INSTANCE.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = CXBaseApplication.INSTANCE.resources.getDimensionPixelSize(resourceId)
            }
            return statusBarHeight
        }

    /* 获取屏幕statusBar高度 */
    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0

        try {
            val clazz = Class.forName("com.android.internal.R\$dimen")
            val `object` = clazz.newInstance()
            val field = clazz.getField("status_bar_height")
            val id = Integer.parseInt(field.get(`object`).toString())
            statusBarHeight = context.resources.getDimensionPixelSize(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return statusBarHeight
    }

    /**
     * 复制到剪贴板
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun copyToClipboard(label: String, contentText: String): Boolean {
        try {
            val cm = CXBaseApplication.INSTANCE.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, contentText)
            cm.primaryClip = clip
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    /*
    https://developer.android.com/training/system-ui/immersive.html#sticky
    https://developer.android.com/training/system-ui/navigation.html

    This snippet hides the system bars.

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            MSystemUtil.hideSystemUI(getWindow(),false);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.launch_activity);


        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            if (hasFocus) {
                MSystemUtil.hideSystemUI(getWindow(),false);
            }
        }

     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun hideSystemUI(window: Window, useImmersiveSticky: Boolean) {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        if (useImmersiveSticky) View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY else View.SYSTEM_UI_FLAG_IMMERSIVE
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun showSystemUI(window: Window) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    val SDK_INT: Int
        get() = Build.VERSION.SDK_INT

    val versionCode: Int
        get() {
            var versionCode = 0
            try {
                versionCode = CXBaseApplication.INSTANCE.packageManager.getPackageInfo(CXBaseApplication.INSTANCE.packageName, 0).versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            return versionCode
        }

    val versionName: String?
        get() {
            var versionName: String? = null
            try {
                versionName = CXBaseApplication.INSTANCE.packageManager.getPackageInfo(CXBaseApplication.INSTANCE.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return versionName
        }

    fun getAppMetaData(key: String): Any? {
        var metaData: Any? = null
        try {
            val info = CXBaseApplication.INSTANCE.packageManager.getApplicationInfo(CXBaseApplication.INSTANCE.packageName, PackageManager.GET_META_DATA)
            metaData = info.metaData.get(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return metaData
    }

    fun getAppMetaDataForInt(key: String): Int {
        var metaData = 0
        try {
            val info = CXBaseApplication.INSTANCE.packageManager.getApplicationInfo(CXBaseApplication.INSTANCE.packageName, PackageManager.GET_META_DATA)
            metaData = info.metaData.getInt(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return metaData
    }

    fun isAppInstalled(context: Context?, pkgName: String): Boolean {
        if (context != null) {
            try {
                context.packageManager.getPackageInfo(pkgName, PackageManager.PERMISSION_GRANTED)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

        }
        return false
    }

    val isAppInBackground: Boolean
        get() {
            return !CXActivityLifecycleCallbacks.isApplicationInForeground
        }

    /**
     * 收起下拉通知栏
     */
    @RequiresPermission(value = "android.permission.EXPAND_STATUS_BAR")
    fun closeStatusBar() {
        try {
            val statusBarManager = CXBaseApplication.INSTANCE.getSystemService("statusbar")
            val collapse: Method
            if (Build.VERSION.SDK_INT <= 16) {
                collapse = statusBarManager.javaClass.getMethod("collapse")
            } else {
                collapse = statusBarManager.javaClass.getMethod("collapsePanels")
            }
            collapse.invoke(statusBarManager)
        } catch (localException: Exception) {
            localException.printStackTrace()
        }
    }
}
