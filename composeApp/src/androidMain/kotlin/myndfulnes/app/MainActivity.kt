package myndfulnes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pass activity reference to the DeviceLocker for extreme focus mode
        setActivityForDeviceLocker(this)
        
        setContent {
            App()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-set the activity reference in case it was lost
        setActivityForDeviceLocker(this)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        // Ensure extreme focus mode stays active when we regain focus
        if (hasFocus && MyndfulnessApp.isInExtremeFocusMode) {
            val deviceLocker = getDeviceLocker()
            deviceLocker.lockDevice()
        }
    }
    
    override fun onBackPressed() {
        // Disable back button when in extreme focus mode
        if (!MyndfulnessApp.isInExtremeFocusMode) {
            super.onBackPressed()
        }
        // Otherwise ignore back button press
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}