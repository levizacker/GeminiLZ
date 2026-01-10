/*

MIT License

Copyright (c) 2026 levizack

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen /* ADDED IN VERSION 1.0.1 */
import androidx.webkit.WebSettingsCompat /* ADDED IN VERSION 1.0.1 */
import androidx.webkit.WebViewFeature /* ADDED IN VERSION 1.0.1 */

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var tempImageUri: Uri? = null
    private var isReady = false /* ADDED IN VERSION 1.0.1 */

    // Handling multiple document selection from file system
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileCallback?.onReceiveValue(if (uris.isNullOrEmpty()) null else uris.toTypedArray())
        fileCallback = null
    }

    // Handling image capture fom camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            fileCallback?.onReceiveValue(arrayOf(tempImageUri!!))
        } else {
            fileCallback?.onReceiveValue(null)
        }
        fileCallback = null
    }

    // Simple permission requester for camera access
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        /* ADDED IN VERSION 1.0.1 */
        installSplashScreen()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enabling edge-to-edge display and setting transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_main)

        // Adjusting padding to prevent content from being hidden behind system bars
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        webView = findViewById(R.id.gemini_webview)

        // Configuring cookies to ensure persistent login sessions
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        setupWebView()

        // Load URL only if there's no saved state (to prevent reload on rotation)
        if (savedInstanceState == null) {
            webView.loadUrl("https://gemini.google.com")
        }

        // Requesting camera permission on startup for Gemini's vision features
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        /* ADDED IN VERSION 1.0.1 */
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

        // Basic web settings for modern web apps
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true

        // Caching settings to improve performance and save traffic
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.databaseEnabled = true

        // Preventing insecure content issues
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        /* ADDED IN VERSION 1.0.1 */
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(s, WebSettingsCompat.FORCE_DARK_OFF)
        }

        if (WebViewFeature.isFeatureSupported("ALGORITHMIC_DARKENING")) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, false)
        }

        // Modifying User-Agent to bypass "browser not supported" errors by removing "w" tag
        val defaultUA = s.userAgentString
        s.userAgentString = defaultUA.replace("; wv)", ")").replace("Version/4.0 ", "")

        webView.webViewClient = object : WebViewClient() {
            // Standard WebViewClient; SSL error handling removed for security reasons
        }

        webView.webChromeClient = object : WebChromeClient() {
            // triggering when Gemini request a file chooser
            override fun onShowFileChooser(wv: WebView?, cb: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb

                // creating temp file for camera capture
                val photoFile = File(externalCacheDir, "temp_camera_photo.jpg")
                tempImageUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", photoFile)

                if (p?.isCaptureEnabled == true) {
                    takePictureLauncher.launch(tempImageUri)
                } else {
                    // /* REDACTED IN VERSON 1.0.1: */ launching file picker with support for ALL files instead of just images, pdf etc.
                    selectFileLauncher.launch(arrayOf("*/*"))
                }
                return true
            }

            // Granting web permissions (e.g. camera/mic) when requested by site
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    // Saving and restoring WebView state to handle orientation changes gracefully
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
        webView.restoreState(bundle)
    }

    // Handling back button to navigate within WebView history
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}