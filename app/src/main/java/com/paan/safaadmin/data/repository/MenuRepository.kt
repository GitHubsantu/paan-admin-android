package com.paan.safaadmin.data.repository

import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.AvailabilityRequest
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.apiCallWithMeta
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.domain.model.MenuItem
import com.paan.safaadmin.core.util.Form
import com.paan.safaadmin.data.remote.apiCallUnit
import com.paan.safaadmin.domain.model.ProductForm
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val api: AdminApi,
) {
    private val _items = MutableStateFlow<List<MenuItem>>(emptyList())
    val items: StateFlow<List<MenuItem>> = _items.asStateFlow()

    suspend fun refresh(): Result<Unit> {
        val all = mutableListOf<MenuItem>()
        var page = 1
        while (true) {
            val (list, meta) = apiCallWithMeta { api.products(perPage = PAGE_SIZE, page = page) }
                .getOrElse { return Result.failure(it) }
            all += list.map { it.toDomain() }
            if (page >= (meta?.lastPage ?: 1) || page >= MAX_PAGES) break
            page++
        }
        _items.value = all
        return Result.success(Unit)
    }

    /** Switches instantly; if the server refuses, that one switch is put back. */
    suspend fun setAvailable(id: String, available: Boolean): Result<Unit> {
        setLocal(id, available)
        return apiCall { api.setAvailability(id, AvailabilityRequest(available)) }
            .map { Unit }
            .onFailure { setLocal(id, !available) }
    }

    fun clear() {
        _items.value = emptyList()
    }

    fun find(id: String): MenuItem? = _items.value.firstOrNull { it.id == id }

    suspend fun save(id: String?, f: ProductForm, image: ByteArray?): Result<Unit> {
        val form = Form()
            .text("name", f.name.trim()).text("category_id", f.categoryId).text("price", f.price.toString())
            .text("description", f.description.trim()).text("tag", f.tag.trim())
            .text("prep_minutes", f.prepMinutes.toString()).text("sort_order", f.sortOrder.toString())
            .text("ingredients_json", Json.encodeToString(f.ingredients))
            .flag("is_available", f.isAvailable).flag("is_featured", f.isFeatured).flag("remove_image", f.removeImage)
            .image(image)
        if (id != null) form.text("_method", "PUT")
        val call = if (id == null) apiCall { api.createProduct(form.list()) } else apiCall { api.updateProduct(id, form.list()) }
        return call.map { Unit }.onSuccess { refresh() }
    }

    suspend fun delete(id: String): Result<Unit> =
        apiCallUnit { api.deleteProduct(id) }.onSuccess { _items.update { l -> l.filterNot { it.id == id } } }

    private fun setLocal(id: String, available: Boolean) {
        _items.update { list -> list.map { if (it.id == id) it.copy(isAvailable = available) else it } }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 20
    }
}