# WebView Implementation for Legitimuz SDK

This guide explains how to implement a WebView in an Android application to integrate with the Legitimuz SDK for KYC verification and identity validation, including handling of special events like manual verification.

## Overview

The sample application demonstrates:
- Setting up a WebView in an Android application for Legitimuz SDK integration
- Enabling JavaScript functionality for SDK communication
- Handling camera permissions for document scanning and facial recognition
- Adding JavaScript interfaces for communication between web and native code
- Handling various event types from the Legitimuz SDK, including manual verification events
- Toggling between WebView and standard navigation

## Implementation Steps

### 1. Add WebView to your layout

Add the WebView to your activity layout file:

```xml
<WebView
    android:id="@+id/webview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginTop="?attr/actionBarSize" />
```

### 2. Add required permissions to AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. Set up the WebView in your activity

```kotlin
// Access WebView using ViewBinding
val webView: WebView = binding.webview

// Configure WebView settings
val webSettings: WebSettings = webView.settings
webSettings.javaScriptEnabled = true // Enable JavaScript
webSettings.domStorageEnabled = true // Enable DOM storage
webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW) // Allow mixed content
webSettings.mediaPlaybackRequiresUserGesture = false // Allow media playback without user gesture
```

### 4. Handle runtime permissions

```kotlin
// Check for Camera permission at runtime
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
}

// Handle permission results
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
```

### 5. Implement a comprehensive JavaScript interface for Legitimuz SDK events

```kotlin
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
                Toast.makeText(
                    context, 
                    "Manual verification needed for: $eventName",
                    Toast.LENGTH_LONG
                ).show()
            }
            snackbar.show()
        }
    }
    
    @JavascriptInterface
    fun onMessage(eventName: String, data: String = "") {
        // Generic message handler for any event from the SDK
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
    
    @JavascriptInterface
    fun debugLog(message: String) {
        Log.d(TAG, "JS DEBUG: $message")
    }
}

// Add JavaScript interface to WebView
webView.addJavascriptInterface(WebAppInterface(this), "Android")
```

### 6. Set up WebViewClient with JavaScript event listener injection

```kotlin
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
```

### 7. Set up WebChromeClient for handling camera permissions

```kotlin
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
```

### 8. Load the Legitimuz URL

```kotlin
// For KYC verification
webView.loadUrl("https://demo.legitimuz.com/teste-kyc/")

// For Liveness detection
// webView.loadUrl("https://demo.legitimuz.com/liveness/")
```

### 9. Add test functionality for event testing

```kotlin
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

// Add button to send a test event
binding.fab.setOnClickListener { view ->
    sendTestEvent(webView)
    Snackbar.make(view, "Sent test event", Snackbar.LENGTH_SHORT).show()
}
```

## Understanding Legitimuz SDK Events

The Legitimuz SDK communicates with your application through the `window.postMessage` API. Messages follow this general format:

```javascript
{
  name: 'event-name',  // The type of event (e.g., 'ocr', 'facematch')
  status: 'success' | 'error' | 'manual',  // The status of the event
  data: { ... },  // Additional data related to the event
  refId: 'reference-id'  // Optional reference ID
}
```

### Event Types

The SDK sends various event types based on the verification process:

| Event Name | Description |
|------------|-------------|
| ocr | Document OCR processing has completed |
| facematch | Face matching verification has completed |
| close-modal | The verification modal was closed |
| redirect | A redirect is requested by the service |
| sms-confirmation | SMS verification code status |
| sow | Statement of Work processing status |
| faceindex | Face indexing process status |

### Status Types

Each event can have one of three status values:

1. **success** - The operation completed successfully
2. **error** - The operation failed with an error
3. **manual** - The operation requires manual verification by a human reviewer

### Handling Manual Verification Events

The "manual" status is particularly important as it indicates situations where automated verification couldn't be completed. This may happen in cases like:

- Documents that couldn't be automatically verified
- Face matches with low confidence scores
- Suspicious activities that require human review

Your application should handle these events by:
1. Informing the user that manual verification is needed
2. Providing clear instructions on what to do next
3. Possibly collecting additional information if required

## Running the Application

1. Build and run the application
2. Use the toggle button to show the WebView
3. The WebView will load the Legitimuz SDK
4. Test events using the floating action button

## Troubleshooting

- **Blank WebView**: Ensure internet permissions are added to AndroidManifest.xml
- **Permission denied errors**: Verify camera permissions are properly requested and handled
- **JavaScript errors**: Check that JavaScript is enabled in WebSettings
- **Events not being received**: Confirm that the JavaScript event listener is correctly injected
- **Manual events not working**: Verify the onManualAction method is properly implemented

## Additional Resources

- [Legitimuz SDK Documentation](https://demo.legitimuz.com/docs/)
- [Android WebView Documentation](https://developer.android.com/reference/android/webkit/WebView)
- [WebView Best Practices](https://developer.android.com/guide/webapps/webview)
- [Managing Runtime Permissions](https://developer.android.com/training/permissions/requesting)
- [Window.postMessage API](https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage) 