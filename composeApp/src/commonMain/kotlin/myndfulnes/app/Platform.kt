package myndfulnes.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Interface for platform-specific device locking functionality
 */
interface DeviceLocker {
    /**
     * Locks the device or prevents screen timeout
     */
    fun lockDevice()
    
    /**
     * Unlocks the device or restores normal screen timeout behavior
     */
    fun unlockDevice()
    
    /**
     * Updates the session active state for platform-specific implementations
     */
    fun setSessionActive(active: Boolean)
}

/**
 * Get platform-specific implementation of DeviceLocker
 */
expect fun getDeviceLocker(): DeviceLocker