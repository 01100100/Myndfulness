package myndfulnes.app

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

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
    }
}

/**
 * Android implementation of DeviceLocker using DevicePolicyManager
 */
class AndroidDeviceLocker(private val context: Context) : DeviceLocker {
    override fun lockDevice() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
        
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }
        // Note: If device admin is not active, the screen won't lock
        // The user needs to enable device admin rights in Settings
    }
    
    override fun unlockDevice() {
        // Nothing to do here - Android doesn't allow programmatic unlocking for security reasons
        // The user will need to unlock their device manually
    }
}

actual fun getDeviceLocker(): DeviceLocker = AndroidDeviceLocker(MyndfulnessApp.appContext)