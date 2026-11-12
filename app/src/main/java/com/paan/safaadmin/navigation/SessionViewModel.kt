package com.paan.safaadmin.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.data.repository.AuthRepository
import com.paan.safaadmin.data.repository.CategoryRepository
import com.paan.safaadmin.data.repository.MenuRepository
import com.paan.safaadmin.data.repository.OrderRepository
import com.paan.safaadmin.data.repository.ShopRepository
import com.paan.safaadmin.push.DeviceTokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    auth: AuthRepository,
    private val orders: OrderRepository,
    shop: ShopRepository,
    menu: MenuRepository,
    categories: CategoryRepository,
    private val deviceTokens: DeviceTokenRepository,
    private val deepLinks: DeepLinkHolder,
) : ViewModel() {

    /** null until the saved login has been read. */
    val loggedIn: StateFlow<Boolean?> = auth.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pendingOrderId: StateFlow<String?> = deepLinks.orderId
    fun consumeDeepLink() = deepLinks.consume()

    init {
        viewModelScope.launch {
            auth.session.map { it != null }.distinctUntilChanged().collect { signedIn ->
                if (!signedIn) {
                    orders.clear(); shop.clear(); menu.clear(); categories.clear()
                }
            }
        }
        viewModelScope.launch {
            var wasSignedIn = false
            auth.session.map { it != null }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) {
                    wasSignedIn = true
                    deviceTokens.register()
                } else if (wasSignedIn) {
                    wasSignedIn = false
                    deviceTokens.deleteLocalToken()
                }
            }
        }
    }

    /** Backup refresh while the app is visible. Pushes refresh the queue instantly and ring the alert. */
    suspend fun watchOrders() {
        while (true) {
            orders.refreshActive()
            delay(15_000)
        }
    }
}