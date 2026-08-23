package com.bharatconnect.app.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bharatconnect.app.presentation.auth.AuthViewModel
import com.bharatconnect.app.presentation.auth.LoginScreen
import com.bharatconnect.app.presentation.auth.OtpVerificationScreen
import com.bharatconnect.app.presentation.auth.RegisterScreen
import com.bharatconnect.app.presentation.home.HomeScreen
import com.bharatconnect.app.presentation.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object OtpVerification : Screen("otp_verification/{email}") {
        fun createRoute(email: String): String = "otp_verification/${Uri.encode(email)}"
    }
    object Home : Screen("home")
}

@Composable
fun BharatConnectNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToOtp = { targetEmail ->
                    navController.navigate(Screen.OtpVerification.createRoute(targetEmail))
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val emailArg = backStackEntry.arguments?.getString("email") ?: ""
            val decodedEmail = Uri.decode(emailArg)
            OtpVerificationScreen(
                email = decodedEmail,
                authViewModel = authViewModel,
                onVerificationSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
