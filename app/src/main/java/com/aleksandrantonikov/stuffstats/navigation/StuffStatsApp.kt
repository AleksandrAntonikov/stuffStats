package com.aleksandrantonikov.stuffstats.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.data.*
import com.aleksandrantonikov.stuffstats.ui.item.*

@Composable
fun StuffStatsApp() {
    val context = LocalContext.current.applicationContext
    val model: ItemsViewModel = viewModel(factory = viewModelFactory {
        initializer { ItemsViewModel(RoomItemRepository(StuffStatsDatabase.get(context))) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.saving.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    BackHandler(enabled = busy) { }
    NavHost(navController = nav, startDestination = AppDestination.Home.route) {
        composable(AppDestination.Home.route) {
            ItemList(state, add = { model.clearError(); nav.navigate(AppDestination.AddItem.route) }, open = { model.clearError(); nav.navigate(AppDestination.ItemDetails.routeFor(it)) })
        }
        composable(AppDestination.AddItem.route) {
            ItemEditor(null, busy, error, back = { nav.navigateUp() }, save = { model.save(it) { nav.popBackStack() } })
        }
        listOf(AppDestination.ItemDetails, AppDestination.EditItem).forEach { destination ->
            composable(destination.route, arguments = listOf(navArgument("itemId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("itemId")
                val item = state.items.find { it.id == id }
                if (item == null) {
                    ItemFrame(stringResource(destination.titleRes), { nav.navigateUp() }) {
                        Text(stringResource(if (state.loading) R.string.loading else if (state.failed) R.string.storage_error else R.string.item_missing))
                    }
                } else if (destination == AppDestination.EditItem) {
                    ItemEditor(item, busy, error, back = { nav.navigateUp() }, save = { model.save(it) { nav.popBackStack() } })
                } else {
                    ItemDetails(item, busy, error, back = { nav.navigateUp() }, edit = { model.clearError(); nav.navigate(AppDestination.EditItem.routeFor(item.id)) }, archive = { model.archive(item) { nav.popBackStack() } })
                }
            }
        }
    }
}
