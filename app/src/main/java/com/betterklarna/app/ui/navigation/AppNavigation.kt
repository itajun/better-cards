package com.betterklarna.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.betterklarna.app.ui.screens.*
import com.betterklarna.app.ui.viewmodel.CardViewModel

@Composable
fun AppNavigation(viewModel: CardViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.CardList.route
    ) {
        composable(NavRoutes.CardList.route) {
            CardListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { cardId ->
                    navController.navigate(NavRoutes.CardDetail.createRoute(cardId))
                },
                onNavigateToAddCard = {
                    navController.navigate(NavRoutes.AddEditCard.createRoute())
                },
                onNavigateToScanner = {
                    navController.navigate(NavRoutes.Scanner.route)
                },
                onNavigateToBatchImport = {
                    navController.navigate(NavRoutes.BatchImport.route)
                }
            )
        }

        composable(
            route = NavRoutes.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
            CardDetailScreen(
                cardId = cardId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(NavRoutes.AddEditCard.createRoute(cardId = id))
                }
            )
        }

        composable(
            route = NavRoutes.AddEditCard.route,
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("cardNumber") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("format") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId")
            val cardNumber = backStackEntry.arguments?.getString("cardNumber")
            val format = backStackEntry.arguments?.getString("format")

            AddEditCardScreen(
                cardId = cardId,
                initialNumber = cardNumber,
                initialFormat = format,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onCardSaved = { savedId ->
                    navController.navigate(NavRoutes.CardDetail.createRoute(savedId)) {
                        popUpTo(NavRoutes.CardList.route)
                    }
                }
            )
        }

        composable(NavRoutes.Scanner.route) {
            ScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onBarcodeDetected = { number, format ->
                    navController.navigate(
                        NavRoutes.AddEditCard.createRoute(
                            cardNumber = number,
                            format = format
                        )
                    ) {
                        popUpTo(NavRoutes.Scanner.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.BatchImport.route) {
            BatchImportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onImportComplete = {
                    navController.navigate(NavRoutes.CardList.route) {
                        popUpTo(NavRoutes.CardList.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
