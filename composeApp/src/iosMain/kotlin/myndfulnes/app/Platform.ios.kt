package myndfulnes.app

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

/**
 * iOS implementation of DeviceLocker
 * 
 * Note: iOS doesn't allow completely locking the device programmatically,
 * so we prevent the screen from dimming/turning off by disabling the idle timer
 */
class IOSDeviceLocker : DeviceLocker {
    // Track the session state in iOS
    private var isSessionActive = false
    
    override fun setSessionActive(active: Boolean) {
        // Store session state locally
        isSessionActive = active
    }
    
    override fun lockDevice() {
        // Prevent the screen from turning off by disabling the idle timer
        UIApplication.sharedApplication.setIdleTimerDisabled(true)
    }
    
    override fun unlockDevice() {
        // Re-enable the idle timer to restore normal behavior
        UIApplication.sharedApplication.setIdleTimerDisabled(false)
    }
}

actual fun getDeviceLocker(): DeviceLocker = IOSDeviceLocker()