package dev.mgks.swv.sample

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.mgks.swv.biometric.SwvBiometricGate
import dev.mgks.swv.sample.R

class MainActivity : AppCompatActivity() {

    private lateinit var biometricGate: SwvBiometricGate
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true

        // --- CONFIGURATION OPTIONS ---
        val config = SwvBiometricGate.Config()

        // Custom Title
        // config.title = "Security Check"

        // Custom Subtitle
        // config.subtitle = "Please verify your identity"

        // Allow or Disallow Pattern/PIN fallback
        // config.allowDeviceCredential = true

        // Enable/Disable auto-lock when app goes to background
        config.autoLockOnBackground = true

        // 1. INITIALIZE GATE
        biometricGate = SwvBiometricGate(this, config)

        // 2. INTEGRATE WITH WEBVIEW (Optional)
        // This enables window.Biometric.authenticate() in JavaScript
        biometricGate.integrateWithWebView(webView)

        // Load a test HTML page to verify JS Bridge
        val testHtml = """
            <html>
            <body style='padding: 20px; font-family: sans-serif;'>
                <h1>Biometric Test</h1>
                <button onclick='callAuth()' style='padding: 15px; background: #007bff; color: white; border: none; border-radius: 5px;'>
                    Test JS Authentication
                </button>
                <p id='status'>Status: Ready</p>
                
                <script>
                    function callAuth() {
                        document.getElementById('status').innerText = 'Status: Requesting...';
                        if(window.Biometric) {
                            window.Biometric.authenticate();
                        } else {
                            alert('Biometric object not found');
                        }
                    }
                    
                    // Callbacks
                    if(!window.Biometric) window.Biometric = {};
                    window.Biometric.onAuthSuccess = function() {
                        document.getElementById('status').innerText = 'Status: SUCCESS ✅';
                        document.body.style.backgroundColor = '#d4edda';
                    };
                    window.Biometric.onAuthError = function(err) {
                        document.getElementById('status').innerText = 'Status: ERROR ❌ ' + err;
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, testHtml, "text/html", "utf-8", null)

        // 3. MANUAL LOCK TEST BUTTON
        findViewById<Button>(R.id.btn_manual_lock).setOnClickListener {
            biometricGate.lock()
        }
    }
}