package com.appinspector.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash_screen")
    data object PermissionCheck : Screen("permission_check_screen")
    data object Home : Screen("home_screen")
    data object Monitor : Screen("monitor_screen")
    data object ErrorDetail : Screen("error_detail_screen/{errorId}") {
        fun createRoute(errorId: Long) = "error_detail_screen/$errorId"
    }
}
