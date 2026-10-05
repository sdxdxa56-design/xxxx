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
import com.appinspector.data.local.GitHubCredentials
import com.appinspector.presentation.analysis.ErrorDetailScreen
import com.appinspector.presentation.analysis.ErrorDetailViewModel
import com.appinspector.presentation.github.BranchPickerScreen
import com.appinspector.presentation.github.DownloadProgressScreen
import com.appinspector.presentation.github.GitHubLoginScreen
import com.appinspector.presentation.github.GitHubViewModel
import com.appinspector.presentation.github.RepoListScreen
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

    private val gitHubViewModel: GitHubViewModel by viewModels()

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
                        errorDetailViewModel = errorDetailViewModel,
                        gitHubViewModel = gitHubViewModel
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
    errorDetailViewModel: ErrorDetailViewModel,
    gitHubViewModel: GitHubViewModel
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
                },
                onNavigateToGitHub = {
                    val credentials = GitHubCredentials(AppInspectorApp.instance)
                    if (credentials.isLoggedIn()) {
                        navController.navigate(Screen.RepoList.route)
                    } else {
                        navController.navigate(Screen.GitHubLogin.route)
                    }
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

        composable(Screen.GitHubLogin.route) {
            GitHubLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.RepoList.route) {
                        popUpTo(Screen.GitHubLogin.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.RepoList.route) {
            RepoListScreen(
                onSelectRepo = { owner, repo ->
                    navController.navigate(Screen.BranchPicker.createRoute(owner, repo))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.BranchPicker.route,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner").orEmpty()
            val repo = backStackEntry.arguments?.getString("repo").orEmpty()
            BranchPickerScreen(
                owner = owner,
                repo = repo,
                onSelectBranch = { o, r, branch ->
                    navController.navigate(Screen.DownloadProgress.createRoute(o, r, branch))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DownloadProgress.route,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner").orEmpty()
            val repo = backStackEntry.arguments?.getString("repo").orEmpty()
            val branch = backStackEntry.arguments?.getString("branch").orEmpty()
            DownloadProgressScreen(
                owner = owner,
                repo = repo,
                branch = branch,
                viewModel = gitHubViewModel,
                onDownloadComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
