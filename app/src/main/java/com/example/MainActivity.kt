package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BiodataViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1200) // Brief smooth splash transition
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    val viewModel: BiodataViewModel = viewModel()
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_biodata_logo),
                    contentDescription = "Biodata Maker Logo",
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Biodata Maker",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Marriage & Career Biodata PDF Studio",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF38BDF8),
                strokeWidth = 2.dp
            )
        }
    }
}
