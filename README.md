# WebView Implementation Tutorial

This guide explains how to implement a WebView in an Android application to load and display web content, with special considerations for permissions and JavaScript interaction.

## Overview

The sample application demonstrates:
- Setting up a WebView in an Android application
- Enabling JavaScript functionality
- Handling camera permissions for web content
- Adding JavaScript interfaces for communication between web and native code
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

### 2. Set up the WebView in your activity

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

### 3. Add WebViewClient for handling page navigation

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
    }
}
```

### 4. Set up WebChromeClient for handling permissions

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

### 5. Add Camera permission to AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### 6. Handle runtime permissions

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

### 7. Add JavaScript interface for web-to-native communication

```kotlin
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
```

### 8. Load the URL

```kotlin
webView.loadUrl("https://demo.legitimuz.com/liveness/")
// Alternative URL for KYC verification:
// webView.loadUrl("https://demo.legitimuz.com/teste-kyc/")
```

### 9. Toggle between WebView and Navigation

Add a toggle button in your layout:

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/toggle_webview"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom|start"
    android:layout_marginStart="@dimen/fab_margin"
    android:layout_marginBottom="16dp"
    app:srcCompat="@android:drawable/ic_menu_view" />
```

Add toggle functionality in your activity:

```kotlin
binding.toggleWebview.setOnClickListener {
    if (binding.webview.visibility == View.VISIBLE) {
        binding.webview.visibility = View.GONE
    } else {
        binding.webview.visibility = View.VISIBLE
        setupWebView() // Initialize WebView when shown
    }
}
```

## Running the Application

1. Build and run the application
2. Use the toggle button at the bottom left to switch between the standard navigation and WebView
3. When the WebView is visible, it will load the URL and handle user interactions

## Troubleshooting

- **Blank WebView**: Ensure internet permissions are added to AndroidManifest.xml
- **Permission denied errors**: Verify camera permissions are properly requested and handled
- **JavaScript errors**: Check that JavaScript is enabled in WebSettings
- **Mixed content warnings**: Ensure mixed content mode is properly configured in WebSettings

## Additional Resources

- [Android WebView Documentation](https://developer.android.com/reference/android/webkit/WebView)
- [WebView Best Practices](https://developer.android.com/guide/webapps/webview)
- [Managing Runtime Permissions](https://developer.android.com/training/permissions/requesting) 