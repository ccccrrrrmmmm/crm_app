package ru.greenland.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.greenland.crm.ui.navigation.CrmNavHost
import ru.greenland.crm.ui.theme.GreenlandCrmTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenlandCrmTheme {
                CrmNavHost()
            }
        }
    }
}
