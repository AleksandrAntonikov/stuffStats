package com.aleksandrantonikov.stuffstats.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aleksandrantonikov.stuffstats.ui.screens.HomeScreen
import com.aleksandrantonikov.stuffstats.ui.screens.ItemDetailsPlaceholder
import com.aleksandrantonikov.stuffstats.ui.screens.PlaceholderScreen

private const val PreviewItemId = 1L

@Composable
fun StuffStatsApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
    ) {
        composable(AppDestination.Home.route) {
            HomeScreen(
                onAddItem = { navController.navigate(AppDestination.AddItem.route) },
                onOpenPreview = {
                    navController.navigate(AppDestination.ItemDetails.routeFor(PreviewItemId))
                },
                onOpenDashboard = { navController.navigate(AppDestination.Dashboard.route) },
            )
        }
        composable(AppDestination.AddItem.route) {
            PlaceholderScreen(
                destination = AppDestination.AddItem,
                onBack = navController::navigateUp,
            )
        }
        composable(AppDestination.EditItem.route) {
            PlaceholderScreen(
                destination = AppDestination.EditItem,
                onBack = navController::navigateUp,
            )
        }
        composable(AppDestination.ItemDetails.route) {
            ItemDetailsPlaceholder(
                onBack = navController::navigateUp,
                onAddUsage = {
                    navController.navigate(AppDestination.AddUsage.routeFor(PreviewItemId))
                },
                onViewPhotos = {
                    navController.navigate(AppDestination.PhotoHistory.routeFor(PreviewItemId))
                },
                onEditItem = {
                    navController.navigate(AppDestination.EditItem.routeFor(PreviewItemId))
                },
            )
        }
        composable(AppDestination.AddUsage.route) {
            PlaceholderScreen(
                destination = AppDestination.AddUsage,
                onBack = navController::navigateUp,
            )
        }
        composable(AppDestination.PhotoHistory.route) {
            PlaceholderScreen(
                destination = AppDestination.PhotoHistory,
                onBack = navController::navigateUp,
            )
        }
        composable(AppDestination.Dashboard.route) {
            PlaceholderScreen(
                destination = AppDestination.Dashboard,
                onBack = navController::navigateUp,
            )
        }
    }
}
