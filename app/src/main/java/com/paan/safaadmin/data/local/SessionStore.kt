package com.paan.safaadmin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paan.safaadmin.domain.model.AdminUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adminStore: DataStore<Preferences> by preferencesDataStore(name = "admin_session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val PHONE = stringPreferencesKey("user_phone")
    }

    val token: Flow<String?> = context.adminStore.data.map { it[Keys.TOKEN] }

    val session: Flow<AdminUser?> = context.adminStore.data.map { p ->
        if (p[Keys.TOKEN] == null) return@map null
        val id = p[Keys.ID] ?: return@map null
        AdminUser(id, p[Keys.NAME].orEmpty().ifBlank { "Admin" }, p[Keys.PHONE].orEmpty())
    }

    suspend fun save(token: String, user: AdminUser) {
        context.adminStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.ID] = user.id
            it[Keys.NAME] = user.name
            it[Keys.PHONE] = user.phone
        }
    }

    suspend fun clear() {
        context.adminStore.edit { it.clear() }
    }

    fun tokenBlocking(): String? = runBlocking { token.first() }
    fun clearBlocking() = runBlocking { clear() }
}