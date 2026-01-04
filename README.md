# Android Biometric Gate

[![](https://jitpack.io/v/mgks/android-biometric-gate.svg)](https://jitpack.io/#mgks/android-biometric-gate)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A secure, lifecycle-aware biometric lock screen for Android. It automatically protects sensitive content by overlaying a lock screen when your app goes into the background, and provides a bridge for WebView apps to trigger authentication from JavaScript.

Extracted from the core of **[Android Smart WebView](https://github.com/mgks/Android-SmartWebView)**.

<img src="https://github.com/mgks/android-biometric-gate/blob/main/preview.gif?raw=true" width="200">

## Features
*   **Auto-Lock:** Automatically locks the Activity when moved to the background (Recents).
*   **Secure:** Uses `FLAG_SECURE` to prevent screenshots/previews in the Recents screen.
*   **Hybrid Ready:** Built-in JavaScript interface (`window.Biometric`) for WebView apps.
*   **Lifecycle Aware:** No manual `onPause`/`onResume` calls required. Just initialize and forget.
*   **Customizable:** Configure titles, subtitles, and fallback behavior.

## Installation

**Step 1. Add JitPack**
```groovy
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

**Step 2. Add Dependency**
```groovy
dependencies {
    implementation 'com.github.mgks:android-biometric-gate:1.0.0'
}
```

## Usage

### 1. Basic (Native Android)
In your `Activity` (e.g., `MainActivity.kt`), just initialize the gate. It handles the rest.

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var biometricGate: SwvBiometricGate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize. That's it. The app is now protected.
        biometricGate = SwvBiometricGate(this)
    }
}
```

### 2. WebView Integration (Hybrid Apps)
If you are building a Hybrid App, you can let your website trigger authentication (e.g., before showing a sensitive transaction).

```kotlin
// 1. Initialize
biometricGate = SwvBiometricGate(this)

// 2. Attach to WebView
val myWebView = findViewById<WebView>(R.id.webview)
biometricGate.integrateWithWebView(myWebView)
```

**JavaScript Usage:**
The library injects a `window.Biometric` object.

```javascript
// Trigger the native prompt
window.Biometric.authenticate();

// Handle Success
window.Biometric.onAuthSuccess = function() {
    console.log("Identity Verified!");
    // Show secret data...
};

// Handle Errors
window.Biometric.onAuthError = function(errorMsg) {
    console.error("Auth Failed: " + errorMsg);
};
```

## Configuration
You can customize the behavior using the `Config` object.

```kotlin
val config = SwvBiometricGate.Config(
    title = "Secure App",
    subtitle = "Verify identity to access",
    autoLockOnBackground = true, // Set false if you only want manual JS triggers
    allowDeviceCredential = true // Allow PIN/Pattern fallback
)

biometricGate = SwvBiometricGate(this, config)
```

## License
MIT

> **{ github.com/mgks }**
> 
> ![Website Badge](https://img.shields.io/badge/Visit-mgks.dev-blue?style=flat&link=https%3A%2F%2Fmgks.dev) ![Sponsor Badge](https://img.shields.io/badge/%20%20Become%20a%20Sponsor%20%20-red?style=flat&logo=github&link=https%3A%2F%2Fgithub.com%2Fsponsors%2Fmgks)
