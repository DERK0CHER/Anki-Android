package net.bueffel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.theme.BueffelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BueffelTheme {
                StudyScreen()
            }
        }
    }
}
