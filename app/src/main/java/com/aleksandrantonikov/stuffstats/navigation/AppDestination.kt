package com.aleksandrantonikov.stuffstats.navigation

import androidx.annotation.StringRes
import com.aleksandrantonikov.stuffstats.R

enum class AppDestination(
    val route: String,
    @get:StringRes val titleRes: Int,
) {
    Home("home", R.string.home_title),
    AddItem("item/add", R.string.add_item_title),
    EditItem("item/{itemId}/edit", R.string.edit_item_title),
    ItemDetails("item/{itemId}", R.string.item_details_title),
    AddUsage("item/{itemId}/usage/add", R.string.add_usage_title),
    PhotoHistory("item/{itemId}/photos", R.string.photo_history_title),
    Dashboard("dashboard", R.string.dashboard),
    ;

    fun routeFor(itemId: Long): String = route.replace("{itemId}", itemId.toString())
}
