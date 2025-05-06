package myndfulnes.app

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    
    private lateinit var deviceLocker: DeviceLocker
    private var screenStateReceiver: BroadcastReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize DeviceLocker
        deviceLocker = getDeviceLocker()
        
        // Pass activity reference to the DeviceLocker
        setActivityForDeviceLocker(this)
        
        // Request necessary permissions
        requestAllPermissions()
        
        // Block back button using the new OnBackPressedDispatcher API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Only allow back press when not in a session
                if (!MyndfulnessApp.isSessionActive) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
                // Otherwise consume the event and do nothing
            }
        })
        
        setContent {
            App()
        }
        
        // Initialize the screen state receiver but don't register yet
        screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        // When screen turns on, bring app to front if in session
                        if (MyndfulnessApp.isSessionActive) {
                            bringToFront()
                            deviceLocker.lockDevice() // Re-enable all restrictions
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Register or unregister the screen state receiver based on session state
     */
    private fun updateScreenReceiverState(active: Boolean) {
        if (active) {
            // Register to receive screen on/off broadcasts
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        } else {
            // Unregister when session ends
            try {
                screenStateReceiver?.let {
                    unregisterReceiver(it)
                }
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }
    }
    
    /**
     * Request all the permissions needed for full functionality
     */
    private fun requestAllPermissions() {
        // Request device admin if needed
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Myndfullness needs device admin permission to block distractions"
            )
            startActivity(intent)
        }
        
        // Request SYSTEM_ALERT_WINDOW permission if needed
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        // Request PACKAGE_USAGE_STATS permission if needed
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        
        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-set the activity reference in case it was lost
        setActivityForDeviceLocker(this)
        
        // Re-lock if we're in a session
        if (MyndfulnessApp.isSessionActive) {
            deviceLocker.lockDevice()
            updateScreenReceiverState(true) // Ensure receiver is registered
            bringToFront() // Make sure we're in the foreground
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Don't unregister the receiver here - we want it to stay active
        // even when our app is in the background, so it can bring our app
        // back to the foreground when the screen turns on
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up the receiver if the activity is being destroyed
        try {
            screenStateReceiver?.let {
                unregisterReceiver(it)
                screenStateReceiver = null
            }
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // If we're in a session, make sure this app is the only one running
        if (MyndfulnessApp.isSessionActive) {
            bringToFront()
        }
    }
    
    /**
     * Bring this app to front if it's not already
     */
    private fun bringToFront() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.getRunningTasks(1)
        if (tasks.isNotEmpty() && tasks[0].topActivity?.packageName != packageName) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        // Ensure focus mode stays active when we regain focus
        if (hasFocus && MyndfulnessApp.isSessionActive) {
            deviceLocker.lockDevice()
            
            // Full screen immersive mode
            if (MyndfulnessApp.isSessionActive) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false)
                    window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
                    window.insetsController?.systemBarsBehavior = 
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
                }
            }
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intercept ALL key events when in a session
        if (MyndfulnessApp.isSessionActive) {
            return true  // Consume all key events
        }
        return super.dispatchKeyEvent(event)
    }
    
    // Keep the legacy onBackPressed for older Android versions
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Always disable back button when in a session
        if (!MyndfulnessApp.isSessionActive) {
            super.onBackPressed()
        }
        // Otherwise trap user in the app
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Always block common hardware keys during a session
        if (MyndfulnessApp.isSessionActive) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_POWER,
                KeyEvent.KEYCODE_BACK -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If user tries to leave (e.g., pressing home button), bring app back to front
        if (MyndfulnessApp.isSessionActive) {
            bringToFront()
        }
    }
    
    /**
     * This method should be called when the session status changes
     * to properly manage the screen receiver
     */
    fun onSessionStatusChanged(active: Boolean) {
        updateScreenReceiverState(active)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}