package myndfulnes.app

import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

/**
 * Application class to provide context for device locking
 */
class MyndfulnessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
        
        // Track if we're in session
        var isSessionActive = false
    }
}

/**
 * Android implementation of DeviceLocker using DevicePolicyManager with extreme focus mode
 */
class AndroidDeviceLocker(private val context: Context) : DeviceLocker {
    private var currentActivity: Activity? = null
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var topOverlayView: View? = null
    private var windowManager: WindowManager? = null
    
    fun setCurrentActivity(activity: Activity) {
        currentActivity = activity
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun setSessionActive(active: Boolean) {
        // Don't do anything if the state hasn't changed
        if (MyndfulnessApp.isSessionActive == active) {
            return
        }
        
        // Update the session state in MyndfulnessApp
        MyndfulnessApp.isSessionActive = active
        
        // Notify MainActivity about the session state change to manage screen receiver
        currentActivity?.let {
            if (it is MainActivity) {
                it.onSessionStatusChanged(active)
            }
        }
        
        // Apply appropriate actions based on new state
        if (active) {
            lockDevice()
        } else {
            unlockDevice()
        }
    }
    
    override fun lockDevice() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
        
        // Check if we have admin permissions, if not request them
        if (!dpm.isAdminActive(adminComponent)) {
            requestDeviceAdminPermission()
            Toast.makeText(
                context, 
                "Please enable Device Admin permission to use Myndfullness", 
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // Always enable extreme focus mode
        enableExtremeFocusMode()
        
        // Add system overlays to block gestures
        addSystemOverlays()
    }
    
    override fun unlockDevice() {
        // Disable focus mode
        disableExtremeFocusMode()
        
        // Remove system overlays
        removeSystemOverlays()
    }
    
    /**
     * Add system overlays that block ALL gestures completely
     */
    private fun addSystemOverlays() {
        try {
            if (overlayView != null) {
                return  // Already added
            }
            
            // 1. Create a full-screen blocking overlay
            overlayView = FrameLayout(context).apply {
                // Block ALL touches except in our app window
                setOnTouchListener { v, event ->
                    // Block touches outside of our app window
                    // This blocks edge swipes and status bar pulldown
                    true
                }
            }
            
            // 2. Create a special status bar overlay focused entirely on the top of the screen
            topOverlayView = FrameLayout(context).apply {
                // Explicitly block status bar pulldown
                setOnTouchListener { v, event -> 
                    // Block ALL touches at the top of the screen
                    true
                }
                
                // Make it visible for debugging (can be removed for production)
                // setBackgroundColor(android.graphics.Color.argb(50, 255, 0, 0))
            }
            
            // Layout parameters for the full-screen overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            
            // Parameters specifically for the top overlay (status bar blocker)
            val topParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                150, // Taller height to ensure it catches ALL top gestures
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            topParams.gravity = Gravity.TOP or Gravity.START
            
            // Add overlays to the window manager in reverse order of importance
            windowManager?.addView(overlayView, params)
            windowManager?.addView(topOverlayView, topParams)
            
            // Schedule a periodic touch blocking check
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (MyndfulnessApp.isSessionActive) {
                        // Temporarily remove and re-add the overlays to ensure they're on top
                        try {
                            windowManager?.removeView(topOverlayView)
                            windowManager?.addView(topOverlayView, topParams)
                        } catch (e: Exception) {
                            // Ignore - view might already be removed
                        }
                        
                        // Reschedule
                        handler.postDelayed(this, 1000) // Check every second
                    }
                }
            }, 1000)
        } catch (e: Exception) {
            // Fail silently - the device might not support system overlays
        }
    }
    
    /**
     * Remove all system overlays
     */
    private fun removeSystemOverlays() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            
            topOverlayView?.let {
                windowManager?.removeView(it)
                topOverlayView = null
            }
        } catch (e: Exception) {
            // Ignore if views were already removed
        }
    }
    
    /**
     * Request device admin permission
     */
    private fun requestDeviceAdminPermission() {
        currentActivity?.let { activity ->
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                      "Myndfullness needs device admin permission to block distractions")
            }
            activity.startActivity(intent)
        }
    }
    
    /**
     * Enable focus mode - hide system UI, prevent navigation, and pin screen
     */
    private fun enableExtremeFocusMode() {
        currentActivity?.let { activity ->
            // Keep screen on
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Make the app full screen
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            // Add flags to prevent screenshots and secure the window
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            
            // Hide system UI with more aggressive flags
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+ use the new WindowInsetsController
                val controller = activity.window.insetsController
                if (controller != null) {
                    // Hide system bars (status and navigation)
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                    
                    // Use the most aggressive behavior to keep bars hidden
                    controller.systemBarsBehavior = 
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                
                // Additional flag to prevent showing system bars when the top/bottom of screen is swiped
                activity.window.setDecorFitsSystemWindows(false)
            } else {
                // For older versions
                @Suppress("DEPRECATION")
                val decorView = activity.window.decorView
                @Suppress("DEPRECATION")
                val uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LOW_PROFILE
                
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = uiOptions
            }
            
            // Set up a recurring task that keeps hiding the system UI in case it becomes visible
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (MyndfulnessApp.isSessionActive) {
                        // Re-hide system UI
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            activity.window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
                        } else {
                            @Suppress("DEPRECATION")
                            val decorView = activity.window.decorView
                            @Suppress("DEPRECATION")
                            val uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                            
                            @Suppress("DEPRECATION")
                            decorView.systemUiVisibility = uiOptions
                        }
                        
                        // Reschedule this task
                        handler.postDelayed(this, 500) // Check every 500ms
                    }
                }
            }, 500)
            
            // Enable screen pinning if possible
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
            
            if (dpm.isAdminActive(adminComponent)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        // First, allow lock task mode on current package
                        val packages = arrayOf(activity.packageName)
                        dpm.setLockTaskPackages(adminComponent, packages)
                        
                        // Start lock task mode
                        activity.startLockTask()
                    } catch (e: Exception) {
                        // Silent catch - we'll continue even if lock task fails
                    }
                }
            }
        }
    }
    
    /**
     * Disable focus mode - restore system UI and normal navigation
     */
    private fun disableExtremeFocusMode() {
        currentActivity?.let { activity ->
            // Allow screen to turn off normally
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Remove fullscreen flag
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            // Remove secure flag
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            
            // Show system UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+ use the new WindowInsetsController
                val controller = activity.window.insetsController
                controller?.show(android.view.WindowInsets.Type.systemBars())
            } else {
                // For older versions
                @Suppress("DEPRECATION")
                val decorView = activity.window.decorView
                @Suppress("DEPRECATION")
                val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = uiOptions
            }
            
            // Disable screen pinning if it was enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    activity.stopLockTask()
                } catch (e: Exception) {
                    // Silent catch - continue even if stopping lock task fails
                }
            }
        }
    }
}

actual fun getDeviceLocker(): DeviceLocker = AndroidDeviceLocker(MyndfulnessApp.appContext)

/**
 * Set the current activity reference for the DeviceLocker
 */
fun setActivityForDeviceLocker(activity: Activity) {
    val deviceLocker = getDeviceLocker()
    if (deviceLocker is AndroidDeviceLocker) {
        deviceLocker.setCurrentActivity(activity)
    }
}