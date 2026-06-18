package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ProfileViewModel
import com.example.ui.ProfileViewModelFactory
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PINScreen
import com.example.ui.screens.ProfileSelectionScreen
import com.example.ui.theme.MyApplicationTheme

// Sealed class for state navigation
sealed class AppScreen {
    object Security : AppScreen()
    object ProfileSelection : AppScreen()
    object Dashboard : AppScreen()
    object AppSelection : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Instantiate the ViewModel using application factory
                val app = application as ProfileManagerApp
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(app, app.repository)
                )

                val masterPin by viewModel.masterPin.collectAsState()
                val isAuthenticated by viewModel.isAuthenticated.collectAsState()
                
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Security) }

                // Auto-route to security selection if PIN is setup, otherwise default to Profile list
                LaunchedEffect(masterPin) {
                    if (masterPin == null) {
                        currentScreen = AppScreen.ProfileSelection
                    } else if (!isAuthenticated) {
                        currentScreen = AppScreen.Security
                    }
                }

                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated && currentScreen == AppScreen.Security) {
                        currentScreen = AppScreen.ProfileSelection
                    }
                }

                // Handle back button presses gracefully across states
                BackHandler(enabled = currentScreen != AppScreen.ProfileSelection && currentScreen != AppScreen.Security) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> {
                            currentScreen = AppScreen.ProfileSelection
                        }
                        AppScreen.AppSelection -> {
                            currentScreen = AppScreen.Dashboard
                        }
                        else -> {
                            currentScreen = AppScreen.ProfileSelection
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transitions"
                        ) { screen ->
                            when (screen) {
                                is AppScreen.Security -> {
                                    PINScreen(
                                        viewModel = viewModel,
                                        onAuthSuccess = {
                                            currentScreen = AppScreen.ProfileSelection
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is AppScreen.ProfileSelection -> {
                                    ProfileSelectionScreen(
                                        viewModel = viewModel,
                                        onProfileSelected = { profile ->
                                            viewModel.selectProfile(profile)
                                            currentScreen = AppScreen.Dashboard
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is AppScreen.Dashboard -> {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToAppSelection = {
                                            currentScreen = AppScreen.AppSelection
                                        },
                                        onNavigateBackToProfiles = {
                                            currentScreen = AppScreen.ProfileSelection
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is AppScreen.AppSelection -> {
                                    AppSelectionScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = {
                                            currentScreen = AppScreen.Dashboard
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
