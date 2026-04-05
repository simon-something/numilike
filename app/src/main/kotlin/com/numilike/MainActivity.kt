package com.numilike

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.numilike.ui.calculator.CalculatorScreen
import com.numilike.ui.settings.SettingsScreen
import com.numilike.ui.theme.NumiLikeTheme
import com.numilike.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            NumiLikeTheme(themeMode = themeMode) {
                val showSettings by viewModel.showSettings.collectAsState()
                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.hideSettings() },
                    )
                } else {
                    CalculatorScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                viewModel.importText(sharedText)
            }
        }
    }
}
