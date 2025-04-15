package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.ui.navigateUp
import com.example.myapplication.databinding.ActivityMainBinding
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Set WebView as initially invisible
        binding.webview.visibility = View.GONE
        
        // Add toggle button functionality
        binding.toggleWebview.setOnClickListener {
            if (binding.webview.visibility == View.VISIBLE) {
                binding.webview.visibility = View.GONE
            } else {
                binding.webview.visibility = View.VISIBLE
                
                // Initialize WebView if it's being shown
                setupWebView()
            }
        }

        // Check for Camera permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

    }

    // Move WebView setup to a separate method
    private fun setupWebView() {
        val webView: WebView = binding.webview

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript
        webSettings.domStorageEnabled = true // Enable DOM storage
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW) // Allow mixed content
        webSettings.mediaPlaybackRequiresUserGesture = false // Allow media playback without user gesture

        // Set WebViewClient to handle internal links
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("WebView Error", "Error: ${error?.description}")
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WebView Host", "Host: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView Host", "Finished loading: $url")
            }
        }

        // WebChromeClient to handle camera permissions
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request != null) {
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        request.grant(request.resources)
                    } else {
                        super.onPermissionRequest(request)
                    }
                }
            }
        }

        // JavaScript interface to handle SDK success and error events
        class WebAppInterface(private val context: MainActivity) {
            @JavascriptInterface
            fun onSuccess(eventName: String) {
                // Handle success event
                Log.d("SDK Success", "Event: $eventName")
            }

            @JavascriptInterface
            fun onError(eventName: String) {
                // Handle error event
                Log.d("SDK Error", "Event: $eventName")
            }
        }

        // Add JavaScript interface to WebView
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // Load the URL of the domain (for example, a remote SDK page)
        webView.loadUrl("https://demo.legitimuz.com/liveness/") // Replace with your actual URL
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now use the camera
                Log.d("Permission", "Camera permission granted.")
            } else {
                // Permission denied, handle appropriately
                Log.d("Permission", "Camera permission denied.")
            }
        }
    }
}