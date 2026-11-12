package com.paan.safaadmin.data.repository

import com.paan.safaadmin.data.local.SessionStore
import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.AdminLoginRequest
import com.paan.safaadmin.data.remote.ApiException
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.apiCallUnit
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.domain.model.AdminUser
import com.paan.safaadmin.push.DeviceTokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AdminApi,
    private val store: SessionStore,
    private val deviceTokens: DeviceTokenRepository,
) {
    val session: Flow<AdminUser?> = store.session

    suspend fun login(phone: String, password: String): Result<AdminUser> =
        apiCall { api.login(AdminLoginRequest(phone, password)) }
            .map { data ->
                val user = data.user.toDomain()
                store.save(data.token, user)
                user
            }
            .recoverCatching { e ->
                // a 401 here means wrong credentials, not an expired session
                if (e is ApiException && e.httpCode == 401) throw ApiException("Incorrect phone number or password.", 401)
                throw e
            }

    suspend fun logout() {
        deviceTokens.unregisterFromServer()
        apiCallUnit { api.logout() }   // best effort
        store.clear()
    }
}