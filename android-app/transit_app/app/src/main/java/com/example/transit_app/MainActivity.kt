package com.example.transit_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.example.transit_app.app.presentation.home.HomeScreen
import com.example.transit_app.ui.theme.TransitAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            TransitAppTheme {
                HomeScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TransitAppTheme {
        HomeScreen()
    }
}
