package com.paan.safaadmin.data.repository

import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.BlockRequest
import com.paan.safaadmin.data.remote.CustomerRequest
import com.paan.safaadmin.data.remote.PageMeta
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.apiCallUnit
import com.paan.safaadmin.data.remote.apiCallWithMeta
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.domain.model.CustomerDetail
import com.paan.safaadmin.domain.model.CustomerForm
import com.paan.safaadmin.domain.model.CustomerRow
import com.paan.safaadmin.domain.model.DeletedCustomer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val api: AdminApi,
) {
    private val _version = MutableStateFlow(0)

    /** Goes up after every change, so the list and detail screens know to reload. */
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun changed() = _version.update { it + 1 }

    suspend fun page(search: String, page: Int): Result<Pair<List<CustomerRow>, PageMeta?>> =
        apiCallWithMeta { api.customers(search.trim().ifEmpty { null }, PAGE_SIZE, page) }
            .map { (list, meta) -> list.map { it.toDomain() } to meta }

    suspend fun detail(id: String): Result<CustomerDetail> =
        apiCall { api.customer(id) }.map { it.toDomain() }

    suspend fun save(id: String?, f: CustomerForm): Result<CustomerRow> {
        val body = CustomerRequest(f.name.trim(), f.phone.trim(), f.email.trim())
        val call = if (id == null) apiCall { api.createCustomer(body) } else apiCall { api.updateCustomer(id, body) }
        return call.map { it.toDomain() }.onSuccess { changed() }
    }

    suspend fun setBlocked(id: String, blocked: Boolean, reason: String?): Result<CustomerRow> =
        apiCall {
            api.blockCustomer(id, BlockRequest(isActive = !blocked, reason = reason?.trim()?.takeIf { it.isNotEmpty() }))
        }.map { it.toDomain() }.onSuccess { changed() }

    suspend fun delete(id: String): Result<Unit> =
        apiCallUnit { api.deleteCustomer(id) }.onSuccess { changed() }

    suspend fun deletedPage(page: Int): Result<Pair<List<DeletedCustomer>, PageMeta?>> =
        apiCallWithMeta { api.deletedCustomers(null, PAGE_SIZE, page) }
            .map { (list, meta) -> list.map { it.toDomain() } to meta }

    suspend fun restore(id: String): Result<CustomerRow> =
        apiCall { api.restoreCustomer(id) }.map { it.toDomain() }.onSuccess { changed() }

    suspend fun purge(id: String): Result<Unit> = apiCallUnit { api.purgeCustomer(id) }
    private companion object {
        const val PAGE_SIZE = 30
    }
}