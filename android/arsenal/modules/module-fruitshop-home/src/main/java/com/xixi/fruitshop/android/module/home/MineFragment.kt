package com.xixi.fruitshop.android.module.home

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.qihoo360.replugin.RePlugin
import com.xixi.library.android.base.FSActivity
import com.xixi.library.android.base.FSBaseFragment


class MineFragment : FSBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val textView: TextView = TextView(activity)
        textView.text = "mine"
        textView.setBackgroundColor(resources.getColor(R.color.fs_pink))
        textView.setOnClickListener {
            /*FSRouteManager.goToFragment(activity, "com.xixi.fruitshop.android.module.mine.MineFragment") {
                FSLogUtil.w("krmao", it.toString())
            }*/
            // 刻意以“Alias（别名）”来打开
            /*val intent = Intent()
            intent.component = ComponentName("demo1", "com.qihoo360.replugin.sample.demo1.activity.for_result.ForResultActivity")
            RePlugin.startActivityForResult(activity, intent, 0, null)*/

            //注册相关Fragment的类
            //注册一个全局Hook用于拦截系统对XX类的寻找定向到Demo1中的XX类主要是用于在xml中可以直接使用插件中的类
            RePlugin.registerHookingClass("com.xixi.fruitshop.android.module.mine.MineFragment", RePlugin.createComponentName("mine", "com.xixi.fruitshop.android.module.mine.MineFragment"), null);
            //代码使用插件Fragment
            val d1ClassLoader = RePlugin.fetchClassLoader("mine")//获取插件的ClassLoader
            try {
                val fragment = d1ClassLoader.loadClass("com.xixi.fruitshop.android.module.mine.MineFragment").asSubclass<Fragment>(Fragment::class.java).newInstance()//使用插件的Classloader获取指定Fragment实例
                FSActivity.start(activity, fragment.javaClass)
            } catch (e: Fragment.InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }


        }
        return textView
    }

    override fun onStart() {
        super.onStart()
        Log.w("krmao", "MineFragment:onStart");
    }

    override fun onStop() {
        super.onStop()
        Log.w("krmao", "MineFragment:onStop");
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w("krmao", "MineFragment:onDestroy");
    }
}
