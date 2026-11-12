package com.paan.safaadmin.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paan.safaadmin.ui.components.AdminTab
import com.paan.safaadmin.ui.components.PaanBackground
import com.paan.safaadmin.ui.components.PaanLogo
import com.paan.safaadmin.ui.screens.alerts.AlertSetupScreen
import com.paan.safaadmin.ui.screens.catalog.CategoriesScreen
import com.paan.safaadmin.ui.screens.catalog.CategoryFormScreen
import com.paan.safaadmin.ui.screens.catalog.ProductFormScreen
import com.paan.safaadmin.ui.screens.customers.CustomerBinScreen
import com.paan.safaadmin.ui.screens.customers.CustomerDetailScreen
import com.paan.safaadmin.ui.screens.customers.CustomerFormScreen
import com.paan.safaadmin.ui.screens.customers.CustomersScreen
import com.paan.safaadmin.ui.screens.dashboard.DashboardScreen
import com.paan.safaadmin.ui.screens.detail.OrderDetailScreen
import com.paan.safaadmin.ui.screens.login.LoginScreen
import com.paan.safaadmin.ui.screens.menu.MenuScreen
import com.paan.safaadmin.ui.screens.queue.QueueScreen
import com.paan.safaadmin.ui.screens.shop.ShopScreen
import kotlinx.coroutines.flow.first

private val optionalId = listOf(
    navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
)

@Composable
fun AdminNavHost() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val loggedIn by sessionViewModel.loggedIn.collectAsStateWithLifecycle()

    // The first known login state decides the start screen; it is fixed once known.
    val startLoggedIn = remember(loggedIn != null) { loggedIn }

    if (startLoggedIn == null) {
        PaanBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PaanLogo() }
        }
    } else {
        AppNav(startLoggedIn = startLoggedIn, loggedIn = loggedIn == true, session = sessionViewModel)
    }
}

@Composable
private fun AppNav(startLoggedIn: Boolean, loggedIn: Boolean, session: SessionViewModel) {
    val nav = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Signing in moves to the queue; signing out (or an expired token) moves back to login.
    LaunchedEffect(loggedIn) {
        val route = nav.currentDestination?.route ?: return@LaunchedEffect
        if (loggedIn && route == Route.Login.path) {
            nav.navigate(Route.Queue.path) { popUpTo(Route.Login.path) { inclusive = true } }
        } else if (!loggedIn && route != Route.Login.path) {
            nav.navigate(Route.Login.path) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    // While signed in and the app is visible: backup refresh of the queue (pushes do the instant work).
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { session.watchOrders() }
        }
    }

    // A tapped notification opens that order once the app is past login.
    val pendingOrder by session.pendingOrderId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOrder, loggedIn) {
        val id = pendingOrder ?: return@LaunchedEffect
        if (!loggedIn) return@LaunchedEffect
        nav.currentBackStackEntryFlow.first { it.destination.route.let { r -> r != null && r != Route.Login.path } }
        nav.navigate(Route.Detail.create(id))
        session.consumeDeepLink()
    }

    /** Bottom-bar navigation: tabs replace each other, the queue stays at the bottom of the stack. */
    val onTabSelected: (AdminTab) -> Unit = { tab ->
        if (tab == AdminTab.Orders) {
            nav.popBackStack(Route.Queue.path, inclusive = false)
        } else {
            val route = when (tab) {
                AdminTab.Dashboard -> Route.Dashboard
                AdminTab.Menu -> Route.Menu
                else -> Route.Shop
            }
            nav.navigate(route.path) {
                popUpTo(Route.Queue.path) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = if (startLoggedIn) Route.Queue.path else Route.Login.path,
    ) {
        composable(Route.Login.path) { LoginScreen() }

        composable(Route.Queue.path) {
            QueueScreen(
                onOpenOrder = { id -> nav.navigate(Route.Detail.create(id)) },
                onTabSelected = onTabSelected,
            )
        }

        composable(
            Route.Detail.path,
            arguments = listOf(navArgument(Route.Detail.ARG_ID) { type = NavType.StringType }),
        ) {
            OrderDetailScreen(onBack = { nav.popBackStack() })
        }

        composable(Route.Dashboard.path) { DashboardScreen(onTabSelected = onTabSelected) }

        composable(Route.Menu.path) {
            MenuScreen(
                onTabSelected = onTabSelected,
                onAddProduct = { nav.navigate(Route.ProductForm.create()) },
                onEditProduct = { id -> nav.navigate(Route.ProductForm.create(id)) },
                onOpenCategories = { nav.navigate(Route.CategoryList.path) },
            )
        }
        composable(Route.CategoryList.path) {
            CategoriesScreen(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(Route.CategoryForm.create()) },
                onEdit = { id -> nav.navigate(Route.CategoryForm.create(id)) },
            )
        }
        composable(Route.CategoryForm.path, arguments = optionalId) {
            CategoryFormScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(Route.ProductForm.path, arguments = optionalId) {
            ProductFormScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }

        composable(Route.Shop.path) {
            ShopScreen(
                onTabSelected = onTabSelected,
                onOpenCustomers = { nav.navigate(Route.Customers.path) },
                onOpenAlertSetup = { nav.navigate(Route.AlertSetup.path) },
            )
        }
        composable(Route.AlertSetup.path) { AlertSetupScreen(onBack = { nav.popBackStack() }) }

        composable(Route.Customers.path) {
            CustomersScreen(
                onBack = { nav.popBackStack() },
                onOpenCustomer = { id -> nav.navigate(Route.CustomerDetail.create(id)) },
                onAddCustomer = { nav.navigate(Route.CustomerForm.create()) },
                onOpenBin = { nav.navigate(Route.CustomerBin.path) },
            )
        }
        composable(Route.CustomerBin.path) { CustomerBinScreen(onBack = { nav.popBackStack() }) }
        composable(
            Route.CustomerDetail.path,
            arguments = listOf(navArgument(Route.CustomerDetail.ARG_ID) { type = NavType.StringType }),
        ) {
            CustomerDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Route.CustomerForm.create(id)) },
                onOpenOrder = { id -> nav.navigate(Route.Detail.create(id)) },
            )
        }
        composable(Route.CustomerForm.path, arguments = optionalId) {
            CustomerFormScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
    }
}