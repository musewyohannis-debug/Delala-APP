package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DelalaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully edge-to-edge layout matching modern Android standard
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Intercept Android hardware Back presses to guide wizard steps backward cleanly
                BackHandler(enabled = true) {
                    val handled = viewModel.navigateBack()
                    if (!handled) {
                        finish()
                    }
                }

                // Render our beautiful customized Material 3 container scaffold
                DelalaMasterLayout(viewModel = viewModel) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(
                            targetState = viewModel.currentScreen,
                            label = "delala_screen_crossfade"
                        ) { screen ->
                            when (screen) {
                                is Screen.Welcome -> WelcomeScreen(viewModel = viewModel)
                                is Screen.LanguageSelect -> LanguageSelectScreen(viewModel = viewModel)
                                is Screen.Auth -> AuthScreen(viewModel = viewModel)
                                is Screen.About -> AboutScreen(viewModel = viewModel)
                                is Screen.RegionSelect -> RegionSelectScreen(viewModel = viewModel)
                                is Screen.RoleSelect -> RoleSelectScreen(viewModel = viewModel)
                                is Screen.SellerHome -> SellerHome(viewModel = viewModel)
                                is Screen.BuyerHome -> BuyerHome(viewModel = viewModel)
                                is Screen.ProductDetails -> ProductDetailsScreen(viewModel = viewModel, screen = screen)
                                is Screen.FeedbackAndNotice -> FeedbackAndNoticeScreen(viewModel = viewModel)
                                is Screen.AdminDashboard -> AdminDashboardScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
