package com.smart.library.widget.webview

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.WebView
import com.smart.library.R
import com.smart.library.base.HKActivity
import com.smart.library.base.HKBaseFragment
import com.smart.library.widget.webview.client.HKWebChromeClient
import com.smart.library.widget.webview.client.HKWebViewClient
import kotlinx.android.synthetic.main.hk_fragment_webview.*

@Suppress("unused", "MemberVisibilityCanPrivate")
open class HKWebFragment : HKBaseFragment(), HKBaseFragment.OnBackPressedListener {

    companion object {
        @JvmStatic
        fun goTo(activity: Activity, url: String?) = goTo(activity, url, false)

        @JvmStatic
        fun goTo(activity: Activity, url: String?, hideBackAtFirstPage: Boolean) {
            val bundle = Bundle()
            bundle.putString("url", url)
            bundle.putBoolean("hideBackAtFirstPage", hideBackAtFirstPage)
            HKActivity.start(activity, HKWebFragment::class.java, bundle)
        }
    }

    private val url: String? by lazy {
        arguments?.getString("url", null)
    }
    private val hideBackAtFirstPage: Boolean by lazy {
        arguments?.getBoolean("hideBackAtFirstPage") == true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.hk_fragment_webview, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title_bar.left0BgView.visibility = if (hideBackAtFirstPage) GONE else VISIBLE

        web_view.setWebViewClient(object : HKWebViewClient() {

            @Suppress("OverridingDeprecatedMember", "DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                title_bar.right0Btn.text = ""
                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                title_bar?.progressBar?.progress = 0
                title_bar?.progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(webView: WebView?, url: String?) {
                super.onPageFinished(webView, url)
                title_bar?.progressBar?.progress = 100
                title_bar?.progressBar?.visibility = View.GONE

                if (hideBackAtFirstPage) {
                    if (web_view.canGoBack())
                        title_bar?.left0BgView?.visibility = View.VISIBLE
                    else
                        title_bar?.left0BgView?.visibility = GONE
                }
            }
        })

        web_view.setWebChromeClient(object : HKWebChromeClient() {
            override fun onProgressChanged(_view: WebView, newProgress: Int) {
                title_bar?.progressBar?.progress = newProgress
                if (newProgress >= 100) {
                    title_bar?.progressBar?.visibility = GONE
                } else {
                    if (title_bar?.progressBar?.visibility == GONE)
                        title_bar?.progressBar?.visibility = View.VISIBLE
                }
                super.onProgressChanged(_view, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title_bar?.titleText?.maxEms = 10
                title_bar?.titleText?.text = title
            }
        })

        web_view.loadURL(url)
    }

    override fun onDestroy() {
        web_view.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed(): Boolean {
        if (web_view != null && web_view.canGoBack()) {
            web_view.goBack()
            return true
        }
        return false
    }
}
