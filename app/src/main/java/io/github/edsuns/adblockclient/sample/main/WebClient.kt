package io.github.edsuns.adblockclient.sample.main

import android.graphics.Bitmap
import android.webkit.*
import io.github.edsuns.adfilter.AdFilter

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class WebClient(private val webViewClientListener: WebViewClientListener) : WebViewClient() {

    private val filter = AdFilter.get()

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request!!.url.toString()
        return !URLUtil.isNetworkUrl(url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val result = filter.shouldIntercept(view!!, request!!)
        webViewClientListener.onShouldInterceptRequest(result)
        return result.resourceResponse
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (url != null) {
            if(!url.contains("crichd")){
                if (view != null) {
                    view.stopLoading()
                    view.goBack()
                }
            }
        }
        filter.performScript(view, url)
        webViewClientListener.onPageStarted(url, favicon)
    }
}