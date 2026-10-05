package com.appinspector.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash_screen")
    data object PermissionCheck : Screen("permission_check_screen")
    data object Home : Screen("home_screen")
    data object Monitor : Screen("monitor_screen")
    data object ErrorDetail : Screen("error_detail_screen/{errorId}") {
        fun createRoute(errorId: Long) = "error_detail_screen/$errorId"
    }
    data object GitHubLogin : Screen("github_login")
    data object RepoList : Screen("repo_list")
    data object BranchPicker : Screen("branch_picker/{owner}/{repo}") {
        fun createRoute(owner: String, repo: String) = "branch_picker/$owner/$repo"
    }
    data object DownloadProgress : Screen("download_progress/{owner}/{repo}/{branch}") {
        fun createRoute(owner: String, repo: String, branch: String) = "download_progress/$owner/$repo/$branch"
    }
    data object BuildProgress : Screen("build_progress/{owner}/{repo}/{branch}") {
        fun createRoute(owner: String, repo: String, branch: String) = "build_progress/$owner/$repo/$branch"
    }
}
