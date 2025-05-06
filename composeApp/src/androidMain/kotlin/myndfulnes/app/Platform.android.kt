package myndfulnes.app

import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.WindowManager
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
        
        // Track if we're currently in extreme focus mode
        var isInExtremeFocusMode = false
    }
}

/**
 * Android implementation of DeviceLocker using DevicePolicyManager with extreme focus mode
 */
class AndroidDeviceLocker(private val context: Context) : DeviceLocker {
    private var currentActivity: Activity? = null
    
    fun setCurrentActivity(activity: Activity) {
        currentActivity = activity
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
        
        // Enable extreme focus mode
        enableExtremeFocusMode()
    }
    
    override fun unlockDevice() {
        // Disable extreme focus mode
        disableExtremeFocusMode()
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
     * Enable extreme focus mode - hide system UI, prevent navigation, and pin screen
     */
    private fun enableExtremeFocusMode() {
        currentActivity?.let { activity ->
            // Set flag to track that we're in extreme focus mode
            MyndfulnessApp.isInExtremeFocusMode = true
            
            // Keep screen on
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Hide system UI (immersive sticky mode)
            val decorView = activity.window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    
            decorView.systemUiVisibility = uiOptions
            
            // Enable screen pinning if possible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    activity.startLockTask()
                } catch (e: Exception) {
                    // Screen pinning might not be available or allowed
                }
            }
        }
    }
    
    /**
     * Disable extreme focus mode - restore system UI and normal navigation
     */
    private fun disableExtremeFocusMode() {
        currentActivity?.let { activity ->
            // Clear flag
            MyndfulnessApp.isInExtremeFocusMode = false
            
            // Allow screen to turn off normally
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Show system UI
            val decorView = activity.window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    
            decorView.systemUiVisibility = uiOptions
            
            // Disable screen pinning if it was enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    activity.stopLockTask()
                } catch (e: Exception) {
                    // Screen pinning might not have been active
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