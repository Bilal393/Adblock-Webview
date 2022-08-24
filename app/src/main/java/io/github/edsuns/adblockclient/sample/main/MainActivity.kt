package io.github.edsuns.adblockclient.sample.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.edsuns.adblockclient.sample.R
import io.github.edsuns.adblockclient.sample.databinding.ActivityMainBinding
import io.github.edsuns.adblockclient.sample.hideKeyboard
import io.github.edsuns.adblockclient.sample.main.blocking.BlockingInfoDialogFragment
import io.github.edsuns.adblockclient.sample.settings.SettingsActivity
import io.github.edsuns.adblockclient.sample.smartUrlFilter
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterResult
import io.github.edsuns.adfilter.FilterViewModel
import io.github.edsuns.smoothprogress.SmoothProgressAnimator

class MainActivity : AppCompatActivity(), WebViewClientListener {

    private lateinit var filterViewModel: FilterViewModel

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var progressAnimator: SmoothProgressAnimator

    private lateinit var blockingInfoDialogFragment: BlockingInfoDialogFragment
    lateinit var url:String

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        url = intent.getStringExtra("url").toString()

        val filter = AdFilter.get()
        filterViewModel = filter.viewModel

        val popupMenu = PopupMenu(
            this,
            binding.menuButton,
            Gravity.NO_GRAVITY,
            R.attr.actionOverflowMenuStyle,
            0
        )
        popupMenu.inflate(R.menu.menu_main)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuRefresh -> webView.reload()
                R.id.menuForward -> webView.goForward()
                R.id.menuSettings ->
                    startActivity(Intent(this, SettingsActivity::class.java))
                else -> finish()
            }
            true
        }
        val menuForward = popupMenu.menu.findItem(R.id.menuForward)

        binding.menuButton.setOnClickListener {
            menuForward.isVisible = webView.canGoForward()
            popupMenu.show()
        }

        blockingInfoDialogFragment = BlockingInfoDialogFragment.newInstance()

        binding.countText.setOnClickListener {
            if (isFilterOn()) {
                if (!blockingInfoDialogFragment.isAdded) {// fix `IllegalStateException: Fragment already added` when double click
                    blockingInfoDialogFragment.show(supportFragmentManager, null)
                }
            } else {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        webView = binding.webView
        webView.webViewClient = WebClient(this)
        webView.webChromeClient = ChromeClient(this)
//        webView!!.webChromeClient = WebChromeClient()

        val settings = webView.settings
        settings.javaScriptEnabled = true
//        settings.databaseEnabled = true
//        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(false)
        // Zooms out the content to fit on screen by width. For example, showing images.
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        // enable touch zoom controls
//        settings.builtInZoomControls = true
//        settings.displayZoomControls = false
//        // allow Mixed Content
//        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        filter.setupWebView(webView)
        webView.loadUrl(url.smartUrlFilter() ?: URLUtil.composeSearchUrl(
            url,
            "https://www.bing.com/search?q={}",
            "{}"
        ))

        progressAnimator = SmoothProgressAnimator(binding.loadProgress)


        viewModel.blockingInfoMap.observe(this, { updateBlockedCount() })

        filterViewModel.isEnabled.observe(this, { updateBlockedCount() })

        filterViewModel.enabledFilterCount.observe(this, { updateBlockedCount() })

        filterViewModel.onDirty.observe(this, {
            webView.clearCache(false)
            viewModel.dirtyBlockingInfo = true
            updateBlockedCount()
        })

        if (!filter.hasInstallation) {
            val map = mapOf(
                "AdGuard Base1" to "https://filters.adtidy.org/extension/chromium/filters/1.txt",
                "AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt",
                "EasyPrivacy Lite" to "https://filters.adtidy.org/extension/chromium/filters/118_optimized.txt",
                "AdGuard Tracking Protection" to "https://filters.adtidy.org/extension/chromium/filters/3.txt",
                "AdGuard Annoyances" to "https://filters.adtidy.org/extension/chromium/filters/14.txt",
                "AdGuard Chinese" to "https://filters.adtidy.org/extension/chromium/filters/224.txt",
                "AdGlock plus" to "https://easylist-downloads.adblockplus.org/easylistgermany+easylist.txt",
                "crik" to "https://test.crik.live/hosts.txt",
                "UBLock" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
                "UBLock4" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                "UBLock1" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters-2020.txt",
                "UBLock2" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters-2021.txt"
            )
            for ((key, value) in map) {
                filterViewModel.addFilter(key, value)
            }
            val filters = filterViewModel.filters.value ?: return
            for ((key, _) in filters) {
                filterViewModel.download(key)
            }
//            AlertDialog.Builder(this)
//                .setTitle(R.string.filter_download_title)
//                .setMessage(R.string.filter_download_msg)
//                .setCancelable(true)
//                .setPositiveButton(
//                    android.R.string.ok
//                ) { _, _ ->
//                    val filters = filterViewModel.filters.value ?: return@setPositiveButton
//                    for ((key, _) in filters) {
//                        filterViewModel.download(key)
//                    }
//                }
//                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val bundle = Bundle()
        webView.saveState(bundle)
        outState.putBundle(KEY_WEB_VIEW, bundle)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val bundle = savedInstanceState.getBundle(KEY_WEB_VIEW)
        if (bundle != null) {
            webView.restoreState(bundle)
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        if (url != null) {
            if(!url.contains("crichd")){
                webView.stopLoading()
                webView.goBack()
            }
        }
        runOnUiThread {
            url?.let { viewModel.currentPageUrl.value = it }
            updateBlockedCount()
        }
    }

    override fun progressChanged(newProgress: Int) {
        runOnUiThread {
            webView.url?.let { viewModel.currentPageUrl.value = it }
            progressAnimator.progress = newProgress
            if (newProgress == 10) {
                viewModel.clearDirty()
                updateBlockedCount()
            }
        }
    }

    private fun isFilterOn(): Boolean {
        val enabledFilterCount = filterViewModel.enabledFilterCount.value ?: 0
        return filterViewModel.isEnabled.value == true && enabledFilterCount > 0
    }

    private fun updateBlockedCount() {
        when {
            !isFilterOn() && !filterViewModel.isCustomFilterEnabled() -> {
                binding.countText.text = getString(R.string.off)
            }
            viewModel.dirtyBlockingInfo -> {
                binding.countText.text = getString(R.string.count_none)
            }
            else -> {
                val blockedUrlMap =
                    viewModel.blockingInfoMap.value?.get(viewModel.currentPageUrl.value)?.blockedUrlMap
                binding.countText.text = (blockedUrlMap?.size ?: 0).toString()
            }
        }
    }

    override fun onShouldInterceptRequest(result: FilterResult) {
        viewModel.logRequest(result)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }

    companion object {
        const val KEY_WEB_VIEW = "KEY_WEB_VIEW"
    }
    private inner class MyChrome internal constructor() : WebChromeClient() {
        protected var mFullscreenContainer: FrameLayout? = null
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0

        /**
         * on Exit of full Screen
         */
        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        /**
         * on Enter in full Screen
         */
        override fun onShowCustomView(
            paramView: View,
            paramCustomViewCallback: CustomViewCallback
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
            mOriginalOrientation = requestedOrientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
            window.decorView.systemUiVisibility = 3846
        }
    }
}