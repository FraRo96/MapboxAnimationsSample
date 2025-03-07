package com.fraro.mapboxanimationssample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fraro.mapboxanimationssample.ui.screens.MapboxScreen
import com.fraro.mapboxanimationssample.ui.theme.MapboxAnimationsSampleTheme
import com.mapbox.common.MapboxOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MapboxAnimationsSampleTheme {
                MapboxOptions.accessToken = stringResource(id = R.string.mapbox_secret_token)
                MapboxScreen()
            }
        }
    }
}