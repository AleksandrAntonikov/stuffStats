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
import com.aleksandrantonikov.stuffstats.ui.screens.DashboardScreen

@Composable
fun StuffStatsApp() {
    val context = LocalContext.current.applicationContext
    val database = remember(context) { StuffStatsDatabase.get(context) }
    val photoFiles = remember(context) { PhotoFileStore(context) }
    val model: ItemsViewModel = viewModel(factory = viewModelFactory {
        initializer {
            ItemsViewModel(
                RoomItemRepository(database),
                RoomUsageEventRepository(database),
                RoomItemPhotoRepository(database),
                photoFiles,
                HealthConnectDistanceSource(context),
            )
        }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.saving.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val distanceImport by model.distanceImport.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    BackHandler(enabled = busy) { }
    NavHost(navController = nav, startDestination = AppDestination.Home.route) {
        composable(AppDestination.Home.route) {
            ItemList(
                state,
                add = { model.clearError(); nav.navigate(AppDestination.AddItem.route) },
                open = { model.clearError(); nav.navigate(AppDestination.ItemDetails.routeFor(it)) },
                dashboard = { model.clearError(); nav.navigate(AppDestination.Dashboard.route) },
                photoFile = model::photoFile,
            )
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
                    ItemDetails(
                        item = item,
                        events = state.eventsFor(item.id),
                        stats = state.statsFor(item),
                        busy = busy,
                        error = error,
                        back = { nav.navigateUp() },
                        edit = { model.clearError(); nav.navigate(AppDestination.EditItem.routeFor(item.id)) },
                        addUsage = { model.clearError(); nav.navigate(AppDestination.AddUsage.routeFor(item.id)) },
                        importDistance = if (!item.isArchived && item.metric.type == com.aleksandrantonikov.stuffstats.domain.MetricType.DISTANCE) {
                            { nav.navigate(AppDestination.ImportDistance.routeFor(item.id)) }
                        } else {
                            null
                        },
                        editUsage = { model.clearError(); nav.navigate(AppDestination.EditUsage.routeFor(item.id, it)) },
                        photos = state.photosFor(item.id),
                        addPhoto = { model.clearError(); nav.navigate(AppDestination.AddPhoto.routeFor(item.id)) },
                        viewPhotos = { model.clearError(); nav.navigate(AppDestination.PhotoHistory.routeFor(item.id)) },
                        photoFile = model::photoFile,
                        archive = { model.archive(item) { nav.popBackStack() } },
                    )
                }
            }
        }
        composable(
            AppDestination.AddUsage.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { entry ->
            val item = state.items.find { it.id == entry.arguments?.getLong("itemId") }
            if (item == null) {
                ItemFrame(stringResource(R.string.add_usage_title), { nav.navigateUp() }) {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.item_missing))
                }
            } else {
                UsageEditor(item, null, busy, error, back = { nav.navigateUp() }, save = { model.saveUsage(it) { nav.popBackStack() } })
            }
        }
        composable(
            AppDestination.ImportDistance.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { entry ->
            val item = state.items.find { it.id == entry.arguments?.getLong("itemId") }
            if (item == null) {
                ItemFrame(stringResource(R.string.import_distance_title), { nav.navigateUp() }) {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.item_missing))
                }
            } else {
                DistanceImportScreen(
                    item = item,
                    state = distanceImport,
                    back = { nav.navigateUp() },
                    prepare = { model.prepareDistanceImport(item) },
                    permissionResult = { model.onDistancePermissionResult(item.id, it) },
                    import = { model.importDistance(item, it) },
                )
            }
        }
        composable(
            AppDestination.EditUsage.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType },
                navArgument("eventId") { type = NavType.LongType },
            ),
        ) { entry ->
            val itemId = entry.arguments?.getLong("itemId")
            val item = state.items.find { it.id == itemId }
            val event = state.events.find { it.id == entry.arguments?.getLong("eventId") && it.itemId == itemId }
            if (item == null || event == null) {
                ItemFrame(stringResource(R.string.edit_usage_title), { nav.navigateUp() }) {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.usage_missing))
                }
            } else {
                UsageEditor(
                    item = item,
                    event = event,
                    busy = busy,
                    error = error,
                    back = { nav.navigateUp() },
                    save = { model.saveUsage(it) { nav.popBackStack() } },
                    delete = { model.deleteUsage(event) { nav.popBackStack() } },
                )
            }
        }
        composable(
            AppDestination.AddPhoto.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { entry ->
            val item = state.items.find { it.id == entry.arguments?.getLong("itemId") }
            if (item == null) {
                ItemFrame(stringResource(R.string.add_photo_title), { nav.navigateUp() }) {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.item_missing))
                }
            } else {
                PhotoEditor(
                    item = item,
                    busy = busy,
                    error = error,
                    back = { nav.navigateUp() },
                    newCapture = model::newPhotoCapture,
                    discardCapture = model::discardPhotoCapture,
                    save = { uri, token, date, notes ->
                        model.savePhoto(item, uri, token, date, notes) { nav.popBackStack() }
                    },
                )
            }
        }
        composable(
            AppDestination.PhotoHistory.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { entry ->
            val item = state.items.find { it.id == entry.arguments?.getLong("itemId") }
            if (item == null) {
                ItemFrame(stringResource(R.string.photo_history_title), { nav.navigateUp() }) {
                    Text(stringResource(if (state.loading) R.string.loading else R.string.item_missing))
                }
            } else {
                PhotoHistory(
                    item = item,
                    photos = state.photosFor(item.id),
                    busy = busy,
                    error = error,
                    back = { nav.navigateUp() },
                    add = { model.clearError(); nav.navigate(AppDestination.AddPhoto.routeFor(item.id)) },
                    delete = { model.deletePhoto(it) { } },
                    photoFile = model::photoFile,
                )
            }
        }
        composable(AppDestination.Dashboard.route) {
            DashboardScreen(
                state = state,
                back = { nav.navigateUp() },
                openItem = { model.clearError(); nav.navigate(AppDestination.ItemDetails.routeFor(it)) },
            )
        }
    }
}
