package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.ZenithViewModel
import com.example.ui.theme.ZenithFinanceTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ZenithViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
      ZenithFinanceTheme(darkTheme = isDarkTheme) {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

