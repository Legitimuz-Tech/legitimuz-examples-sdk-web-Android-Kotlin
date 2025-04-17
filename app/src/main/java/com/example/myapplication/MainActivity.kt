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
            private val TAG = "LegitimuzSDK"
            
            @JavascriptInterface
            fun onSuccess(eventName: String) {
                // Handle success event
                Log.i(TAG, "SUCCESS EVENT: $eventName")
                context.runOnUiThread {
                    Snackbar.make(context.binding.root, "Success: $eventName", Snackbar.LENGTH_SHORT).show()
                }
            }

            @JavascriptInterface
            fun onError(eventName: String, errorMessage: String = "") {
                // Handle error event
                Log.e(TAG, "ERROR EVENT: $eventName, Error Message: $errorMessage")
                context.runOnUiThread {
                    Snackbar.make(context.binding.root, "Error: $eventName", Snackbar.LENGTH_SHORT).show()
                }
            }
            
            @JavascriptInterface
            fun onManualAction(eventName: String, data: String = "") {
                // Handle manual status events that require user intervention
                Log.i(TAG, "MANUAL ACTION REQUIRED: $eventName")
                Log.i(TAG, "Event Data: $data")
                
                context.runOnUiThread {
                    // Show a more distinctive message for manual events
                    val snackbar = Snackbar.make(
                        context.binding.root, 
                        "Manual verification required: $eventName", 
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction("Details") {
                        // Show details dialog or additional information
                        android.widget.Toast.makeText(
                            context, 
                            "Manual verification needed for: $eventName",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    snackbar.show()
                }
            }
            
            @JavascriptInterface
            fun onMessage(eventName: String, data: String = "") {
                // Generic message handler for any event from the website
                Log.i(TAG, "MESSAGE RECEIVED: $eventName")
                Log.i(TAG, "Event Data: $data")
                
                context.runOnUiThread {
                    when (eventName) {
                        "close-modal" -> Snackbar.make(context.binding.root, "Modal closed", Snackbar.LENGTH_SHORT).show()
                        "ocr" -> Snackbar.make(context.binding.root, "Document scanned", Snackbar.LENGTH_SHORT).show()
                        "facematch" -> Snackbar.make(context.binding.root, "Face matching completed", Snackbar.LENGTH_LONG).show()
                        "redirect" -> Snackbar.make(context.binding.root, "Redirect requested", Snackbar.LENGTH_SHORT).show()
                        "sms-confirmation" -> Snackbar.make(context.binding.root, "SMS verification", Snackbar.LENGTH_LONG).show()
                        "sow" -> Snackbar.make(context.binding.root, "Statement of Work completed", Snackbar.LENGTH_LONG).show()
                        "faceindex" -> Snackbar.make(context.binding.root, "Face indexing completed", Snackbar.LENGTH_LONG).show()
                        else -> Snackbar.make(context.binding.root, "Event: $eventName", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            
            // Add a simple debug method that can be called from JavaScript
            @JavascriptInterface
            fun debugLog(message: String) {
                Log.d(TAG, "JS DEBUG: $message")
            }
        }

        // Add JavaScript interface to WebView
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // Set WebViewClient to handle internal links and inject JavaScript for handling events
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
                
                // Inject JavaScript to listen for postMessage events
                val eventListenerScript = """
                    (function() {
                        console.log('Legitimuz event handler initialized');
                        
                        // Listen for postMessage events
                        window.addEventListener('message', function(event) {
                            console.log('Received message from: ' + event.origin);
                            
                            // Only process messages from legitimuz domains
                            if (event.origin.includes('legitimuz.com')) {
                                try {
                                    var data = event.data;
                                    
                                    // Parse data if it's a string
                                    if (typeof data === 'string') {
                                        try { data = JSON.parse(data); } catch(e) {}
                                    }
                                    
                                    console.log('Processing event data: ' + JSON.stringify(data));
                                    
                                    // Handle event based on format
                                    if (data.name) {
                                        var eventName = data.name;
                                        var status = data.status || '';
                                        
                                        if (status === 'success') {
                                            window.Android.onSuccess(eventName);
                                        } else if (status === 'error') {
                                            window.Android.onError(eventName, data.error || '');
                                        } else if (status === 'manual') {
                                            window.Android.onManualAction(eventName, JSON.stringify(data));
                                        } else {
                                            window.Android.onMessage(eventName, JSON.stringify(data));
                                        }
                                    } else {
                                        window.Android.onMessage('generic_event', JSON.stringify(data));
                                    }
                                } catch(e) {
                                    console.error('Error processing message:', e);
                                    window.Android.debugLog('Error: ' + e.toString());
                                }
                            }
                        });
                        
                        console.log('Legitimuz event listener registered');
                    })();
                """.trimIndent()
                
                view?.evaluateJavascript(eventListenerScript, null)
            }
        }

        // Load the URL of the domain
        webView.loadUrl("https://demo.legitimuz.com/teste-kyc/")
        
        // Add button to send a test event
        binding.fab.setOnClickListener { view ->
            sendTestEvent(webView)
            Snackbar.make(view, "Sent test event", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    // Helper method to send a test event to simulate SDK interactions
    private fun sendTestEvent(webView: WebView) {
        val testScript = """
            (function() {
                console.log('Sending test events from Android');
                
                // Simulate a success event
                window.postMessage({
                    name: 'test_event',
                    status: 'success',
                    data: { source: 'Android app' }
                }, '*');
                
                // Simulate a manual verification event
                window.postMessage({
                    name: 'facematch',
                    status: 'manual',
                    data: { 
                        confidence: 0.65,
                        reason: 'Low confidence score'
                    }
                }, '*');
                
                return "Test events sent";
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(testScript, null)
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