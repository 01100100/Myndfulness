package myndfulnes.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.floor

@Composable
fun App() {
    val deviceLocker = getDeviceLocker()
    var isSessionActive by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(20 * 60) } // 20 minutes in seconds

    // Calm, dark color palette
    val darkBlue = Color(0xFF232946)
    val calmAccent = Color(0xFFB8C1EC)
    val buttonColor = Color(0xFF121629)

    // Timer effect
    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            deviceLocker.lockDevice()
            while (timeLeft > 0 && isSessionActive) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft == 0) {
                isSessionActive = false
                deviceLocker.unlockDevice()
            }
        } else {
            deviceLocker.unlockDevice()
            timeLeft = 20 * 60
        }
    }

    MaterialTheme(colors = darkColors(
        primary = calmAccent,
        background = darkBlue,
        surface = darkBlue,
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBlue),
            contentAlignment = Alignment.Center
        ) {
            if (!isSessionActive) {
                Button(
                    onClick = { isSessionActive = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "🧘", 
                        fontSize = 44.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        color = Color.White
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = timeLeft.let { seconds ->
                            val min = floor(seconds / 60.0).toInt()
                            val sec = seconds % 60
                            "%02d:%02d".format(min, sec)
                        },
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = calmAccent
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { isSessionActive = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Text(
                            "🔓", 
                            fontSize = 36.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}