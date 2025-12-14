package dev.mgks.swv.biometric

import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.mgks.swv.biometric.gate.R

/**
 * Android Biometric Gate.
 * 1. Automatically locks the Activity when backgrounded.
 * 2. Provides a JS Interface for WebViews to trigger auth.
 */
class SwvBiometricGate @JvmOverloads constructor(
    private val activity: AppCompatActivity,
    private val config: Config = Config()
) : DefaultLifecycleObserver {

    private var overlayView: View? = null
    private var isLocked = false
    private var promptInfo: BiometricPrompt.PromptInfo
    private var webView: WebView? = null // For JS callbacks

    /**
     * Configuration options for the Biometric Gate.
     */
    data class Config @JvmOverloads constructor(
        var title: String = "Authentication Required",
        var subtitle: String = "Log in to continue",
        var description: String? = null,
        var autoLockOnBackground: Boolean = true,
        var allowDeviceCredential: Boolean = true // Pattern/PIN fallback
    )

    init {
        activity.lifecycle.addObserver(this)

        var authTypes = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (config.allowDeviceCredential) {
            authTypes = authTypes or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)
            .setSubtitle(config.subtitle)
            .setAllowedAuthenticators(authTypes)

        if (config.description != null) builder.setDescription(config.description)
        promptInfo = builder.build()

        setupOverlay()
    }

    /**
     * Bridges this native gate with a WebView.
     * Injects 'window.Biometric' to allow JS triggers.
     */
    fun integrateWithWebView(webView: WebView) {
        this.webView = webView
        webView.addJavascriptInterface(JsInterface(), "BiometricInterface")
        injectJsHelper(webView)
    }

    private fun setupOverlay() {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(activity)
        overlayView = inflater.inflate(R.layout.swv_biometric_overlay, rootLayout, false)

        rootLayout.addView(overlayView)

        // Setup UI text from config
        overlayView?.findViewById<TextView>(R.id.swv_lock_title)?.text = config.title

        val unlockBtn = overlayView?.findViewById<Button>(R.id.swv_unlock_button)
        unlockBtn?.setOnClickListener { authenticate(isManualRequest = false) }
    }

    fun lock() {
        if (isLocked) return
        isLocked = true
        showOverlay(true)
        authenticate(isManualRequest = false)
    }

    /**
     * @param isManualRequest True if requested via JS or Button (don't lock UI if failed).
     *                        False if App Resume (MUST lock UI).
     */
    private fun authenticate(isManualRequest: Boolean) {
        val biometricManager = BiometricManager.from(activity)

        var authTypes = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (config.allowDeviceCredential) {
            authTypes = authTypes or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuth = biometricManager.canAuthenticate(authTypes)

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                    if (isManualRequest) sendJsCallback("onAuthSuccess")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (isManualRequest) sendJsCallback("onAuthError", "'$errString'")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (isManualRequest) sendJsCallback("onAuthFailed")
                }
            }
            BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
        } else {
            // No security set up?
            if (isManualRequest) {
                sendJsCallback("onAuthError", "'Device not secure'")
            } else {
                // If this is a forced lock (background), force settings
                val enrollIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                activity.startActivity(enrollIntent)
            }
        }
    }

    private fun unlock() {
        isLocked = false
        showOverlay(false)
    }

    private fun showOverlay(show: Boolean) {
        activity.runOnUiThread {
            overlayView?.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun sendJsCallback(method: String, params: String = "") {
        activity.runOnUiThread {
            val script = "if(window.Biometric && window.Biometric.$method) { window.Biometric.$method($params); }"
            webView?.evaluateJavascript(script, null)
        }
    }

    private fun injectJsHelper(view: WebView) {
        // Simple shim to make sure the object exists even if JS interface loads slowly
        val js = """
            if(!window.Biometric){
                window.Biometric = {
                    authenticate: function() { 
                        if(window.BiometricInterface) window.BiometricInterface.authenticate(); 
                    },
                    onAuthSuccess: null,
                    onAuthError: null,
                    onAuthFailed: null
                };
            }
        """.trimIndent()
        // We inject this when requested, but usually, this should be in the page's JS or injected onPageFinished
        view.post { view.evaluateJavascript(js, null) }
    }

    // --- JS Interface ---
    private inner class JsInterface {
        @JavascriptInterface
        fun authenticate() {
            activity.runOnUiThread {
                // Manual request does NOT lock the whole screen overlay, just shows prompt
                this@SwvBiometricGate.authenticate(isManualRequest = true)
            }
        }
    }

    // --- Lifecycle Events ---
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (config.autoLockOnBackground) {
            lock()
        }
    }
}