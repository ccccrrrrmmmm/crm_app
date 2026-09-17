package ru.greenland.crm.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.greenland.crm.ui.clients.ClientDetailScreen
import ru.greenland.crm.ui.clients.ClientFormScreen
import ru.greenland.crm.ui.clients.ClientsListScreen
import ru.greenland.crm.ui.masters.MasterFormScreen
import ru.greenland.crm.ui.masters.MastersListScreen
import ru.greenland.crm.ui.orders.OrderDetailScreen
import ru.greenland.crm.ui.orders.OrderFormScreen
import ru.greenland.crm.ui.orders.OrdersListScreen
import ru.greenland.crm.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmNavHost() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (isTopLevel) {
                TopAppBar(title = { Text("Greenland CRM") })
            }
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ORDERS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ORDERS) {
                OrdersListScreen(
                    onOrderClick = { navController.navigate(Routes.orderDetail(it)) },
                    onAddClick = { navController.navigate(Routes.orderNew()) },
                )
            }
            composable(
                route = Routes.ORDER_NEW_PATTERN,
                arguments = listOf(navArgument("clientId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                OrderFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.ORDER_DETAIL) {
                OrderDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.orderEdit(it)) },
                )
            }
            composable(Routes.ORDER_EDIT) {
                OrderFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.CLIENTS) {
                ClientsListScreen(
                    onClientClick = { navController.navigate(Routes.clientDetail(it)) },
                    onAddClick = { navController.navigate(Routes.CLIENT_NEW) },
                )
            }
            composable(Routes.CLIENT_NEW) {
                ClientFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.CLIENT_DETAIL) {
                ClientDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.clientEdit(it)) },
                    onOrderClick = { navController.navigate(Routes.orderDetail(it)) },
                    onNewOrder = { clientId ->
                        navController.navigate(Routes.orderNew(clientId))
                    },
                )
            }
            composable(Routes.CLIENT_EDIT) {
                ClientFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.MASTERS) {
                MastersListScreen(
                    onMasterClick = { navController.navigate(Routes.masterEdit(it)) },
                    onAddClick = { navController.navigate(Routes.MASTER_NEW) },
                )
            }
            composable(Routes.MASTER_NEW) {
                MasterFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.MASTER_EDIT) {
                MasterFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
