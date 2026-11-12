package com.paan.safaadmin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.paan.safaadmin.navigation.AdminNavHost
import com.paan.safaadmin.navigation.DeepLinkHolder
import com.paan.safaadmin.ui.theme.SafaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var deepLinks: DeepLinkHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { SafaTheme { AdminNavHost() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val id = i?.getStringExtra(EXTRA_ORDER_ID)?.takeIf { it.isNotEmpty() } ?: return
        i.removeExtra(EXTRA_ORDER_ID)
        deepLinks.open(id)
    }

    companion object {
        const val EXTRA_ORDER_ID = "open_order_id"
    }
}