package com.levizack.gemini

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var tempImageUri: Uri? = null
    private var isReady = false

    // handling multiple document selection from file system
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileCallback?.onReceiveValue(if (uris.isNullOrEmpty()) null else uris.toTypedArray())
        fileCallback = null
    }

    // handling image capture fom camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            fileCallback?.onReceiveValue(arrayOf(tempImageUri!!))
        } else {
            fileCallback?.onReceiveValue(null)
        }
        fileCallback = null
    }

    // permission requester for camera access
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // enabling edge-to-edge display and setting transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_main)

        // adjusting padding to prevent content from being hidden behind system bars
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        webView = findViewById(R.id.gemini_webview)

        // configuring cookies to ensure persistent login sessions
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        setupWebView()

        // load url only if theres no saved state (to prevent reload on rotation)
        if (savedInstanceState == null) {
            webView.loadUrl("https://gemini.google.com")
        }

        // requesting camera permission on startup for geminis vision features and something like that
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        splashScreen.setKeepOnScreenCondition {
            !isReady
        }
        rootLayout.postDelayed({
            isReady = true
        }, 1000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings

        // basic web settings for modern web apps
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true

        // caching settings to improve performance and save traffic
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.databaseEnabled = true

        // preventing insecure content issues
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(s, WebSettingsCompat.FORCE_DARK_OFF)
        }

        if (WebViewFeature.isFeatureSupported("ALGORITHMIC_DARKENING")) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, false)
        }

        // modifying User-Agent to bypass "browser not supported" errors by removing "wм" (aka "webview") tag
        val defaultUA = s.userAgentString
        s.userAgentString = defaultUA.replace("; wv)", ")").replace("Version/4.0 ", "")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    webView.evaluateJavascript(
                        "document.documentElement.style.setProperty(\"color-scheme\", \"dark\");",
                        null
                    )
                } else {
                    webView.evaluateJavascript(
                        "document.documentElement.style.setProperty(\"color-scheme\", \"light\");",
                        null
                    )
                }
            }
        }

        // TODO: nothing, thats just most fun part :)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(wv: WebView?, cb: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb

                val southCanada = "US" // :)
                val locale = Locale.getDefault()
                val pattern = if (locale.country == southCanada) "MMddyyyy_HHmmssSSS" else "ddMMyyyy_HHmmssSSS"

                val formatter = SimpleDateFormat(pattern, locale)
                val dateString = formatter.format(Date())

                val fileName = "${dateString}.jbg.jpg" // srpski vibe+ 8)
                val photoFile = File(externalCacheDir, fileName)

                tempImageUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", photoFile)

                if (p?.isCaptureEnabled == true) {
                    takePictureLauncher.launch(tempImageUri)
                } else {
                    selectFileLauncher.launch(arrayOf("*/*"))
                }
                return true
            }


            // granting web permissions (e.g. camera/mic) when requested by site
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    // saving and restoring WebView state to handle orientation changes gracefully
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
        webView.restoreState(bundle)
    }

    // handling back button to navigate within WebView history
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}