package com.xixi.fruitshop.android.module.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.qihoo360.replugin.RePlugin
import com.xixi.library.android.base.FSBaseFragment

class HybirdFragment : FSBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val textView: TextView = TextView(activity)
        textView.text = "hybird"
        textView.setBackgroundColor(Color.parseColor("#FF33B5E5"))
        textView.setOnClickListener {
            /*FSRouteManager.goToFragment(activity, "com.xixi.fruitshop.android.module.hybird.HybirdFragment") {
                FSLogUtil.w("krmao", it.toString())
            }*/
            // 刻意以“包名”来打开
            RePlugin.startActivity(context, RePlugin.createIntent("com.xixi.fruitshop.android.module.hybird", "com.qihoo360.replugin.sample.demo1.MainActivity"));
        }
        return textView
    }

    override fun onStart() {
        super.onStart()
        Log.w("krmao", "HybirdFragment:onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.w("krmao", "HybirdFragment:onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w("krmao", "HybirdFragment:onDestroy")
    }
}
