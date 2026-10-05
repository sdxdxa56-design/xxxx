package com.appinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appinspector.presentation.analysis.ErrorDetailScreen
import com.appinspector.presentation.analysis.ErrorDetailViewModel
import com.appinspector.presentation.home.HomeScreen
import com.appinspector.presentation.home.HomeViewModel
import com.appinspector.presentation.home.SplashScreen
import com.appinspector.presentation.monitor.MonitorScreen
import com.appinspector.presentation.monitor.MonitorViewModel
import com.appinspector.presentation.navigation.Screen
import com.appinspector.presentation.permission.PermissionCheckScreen
import com.appinspector.presentation.theme.AppInspectorTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(AppInspectorApp.instance.repository) as T
            }
        }
    }

    private val monitorViewModel: MonitorViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MonitorViewModel(AppInspectorApp.instance.repository) as T
            }
        }
    }

    private val errorDetailViewModel: ErrorDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ErrorDetailViewModel(AppInspectorApp.instance.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppInspectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        homeViewModel = homeViewModel,
                        monitorViewModel = monitorViewModel,
                        errorDetailViewModel = errorDetailViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    monitorViewModel: MonitorViewModel,
    errorDetailViewModel: ErrorDetailViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(Screen.PermissionCheck.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PermissionCheck.route) {
            PermissionCheckScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionCheck.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToMonitor = {
                    navController.navigate(Screen.Monitor.route)
                },
                onNavigateToDetail = { errorId ->
                    navController.navigate(Screen.ErrorDetail.createRoute(errorId))
                }
            )
        }

        composable(Screen.Monitor.route) {
            MonitorScreen(
                viewModel = monitorViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ErrorDetail.route,
            arguments = listOf(
                navArgument("errorId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val errorId = backStackEntry.arguments?.getLong("errorId") ?: 0L
            ErrorDetailScreen(
                errorId = errorId,
                viewModel = errorDetailViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
