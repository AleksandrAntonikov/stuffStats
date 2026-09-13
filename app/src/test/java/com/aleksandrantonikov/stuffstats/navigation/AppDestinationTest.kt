package com.aleksandrantonikov.stuffstats.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = AppDestination.entries.map(AppDestination::route)

        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun itemRouteReplacesArgument() {
        val route = AppDestination.ItemDetails.routeFor(itemId = 42)

        assertEquals("item/42", route)
        assertTrue("{" !in route)
    }

    @Test
    fun usageEditRouteReplacesBothArguments() {
        assertEquals("item/42/usage/7/edit", AppDestination.EditUsage.routeFor(itemId = 42, eventId = 7))
    }
}
