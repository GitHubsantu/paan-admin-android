package com.paan.safaadmin.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.paan.safaadmin.data.local.SessionStore
import com.paan.safaadmin.data.repository.OrderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaanMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokens: DeviceTokenRepository
    @Inject lateinit var orders: OrderRepository
    @Inject lateinit var store: SessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { if (store.token.first() != null) tokens.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        if (d["type"] != "new_order") return
        AdminAlerts.showNewOrder(this, NewOrderAlert.from(d))
        scope.launch { orders.refreshActive() }   // the queue updates at once if the app is open
    }
}