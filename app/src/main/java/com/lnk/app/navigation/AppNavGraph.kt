package com.lnk.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lnk.app.auth.AuthViewModel
import com.lnk.app.auth.AuthViewModelFactory
import com.lnk.app.data.repository.AuthRepositoryImpl
import com.lnk.app.data.repository.FirestoreRepositoryImpl
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.salary.SalaryViewModelFactory
import com.lnk.app.ui.screens.AppStartScreen
import com.lnk.app.ui.screens.DailySalesInputScreen
import com.lnk.app.ui.screens.LoginScreen
import com.lnk.app.ui.screens.MainScreen
import com.lnk.app.ui.screens.SalaryResultScreen
import com.lnk.app.ui.screens.SalarySettingScreen
import com.lnk.app.ui.screens.SignUpScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Route.AppStart.route
) {
    val authRepository = remember { AuthRepositoryImpl() }
    val firestoreRepository = remember { FirestoreRepositoryImpl() }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val authUiState by authViewModel.uiState.collectAsState()
    val salaryViewModel: SalaryViewModel = viewModel(
        factory = SalaryViewModelFactory(authRepository, firestoreRepository)
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.AppStart.route) {
            AppStartScreen(
                uiState = authUiState,
                onNavigateToLogin = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.AppStart.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.AppStart.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onSignUp = { navController.navigate(Route.SignUp.route) },
                onSuccess = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.SignUp.route) {
            SignUpScreen(
                authViewModel = authViewModel,
                onLogin = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Main.route) {
            MainScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Route.SalarySetting.route) {
            SalarySettingScreen(salaryViewModel = salaryViewModel)
        }
        composable(Route.DailySalesInput.route) {
            DailySalesInputScreen(salaryViewModel = salaryViewModel)
        }
        composable(Route.SalaryResult.route) {
            SalaryResultScreen(salaryViewModel = salaryViewModel)
        }
        composable(Route.ToiletMap.route) {
            PlaceholderScreen(title = "Toilet Map", onBack = { navController.popBackStack() })
        }
        composable(Route.ToiletDetail.route) {
            PlaceholderScreen(title = "Toilet Detail", onBack = { navController.popBackStack() })
        }
        composable(Route.AddToilet.route) {
            PlaceholderScreen(title = "Add Toilet", onBack = { navController.popBackStack() })
        }
        composable(Route.Comment.route) {
            PlaceholderScreen(title = "Comment", onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    onPrimary: (() -> Unit)? = null,
    primaryText: String = "Continue",
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (onPrimary != null) {
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onPrimary
            ) {
                Text(text = primaryText)
            }
        }
        if (onBack != null) {
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = onBack
            ) {
                Text(text = "Back")
            }
        }
    }
}
