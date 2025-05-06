package myndfulnes.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val darkBlue = Color(0xFF0F1629)
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

    // Pulsating animation for the timer
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isSessionActive) 1.1f else 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Myndfullness",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = calmAccent,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { isSessionActive = true },
                        colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .border(4.dp, calmAccent, CircleShape)
                    ) {
                        Text(
                            "🧘", 
                            fontSize = 80.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            color = Color.White
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(40.dp))
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp * pulseAnimation)
                            .clip(CircleShape)
                            .background(darkBlue.copy(alpha = 0.8f))
                            .border(4.dp, calmAccent, CircleShape)
                    ) {
                        Text(
                            text = timeLeft.let { seconds ->
                                val min = floor(seconds / 60.0).toInt()
                                val sec = seconds % 60
                                "%02d:%02d".format(min, sec)
                            },
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(bottom = 40.dp)
                    ) {
                        Button(
                            onClick = { isSessionActive = false },
                            colors = ButtonDefaults.buttonColors(backgroundColor = darkBlue),
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(2.dp, calmAccent, CircleShape)
                        ) {
                            Text(
                                "🔓", 
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}