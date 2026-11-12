package com.paan.safaadmin.data.repository

import com.paan.safaadmin.core.util.Form
import com.paan.safaadmin.data.remote.AdminApi
import com.paan.safaadmin.data.remote.apiCall
import com.paan.safaadmin.data.remote.apiCallUnit
import com.paan.safaadmin.data.remote.toDomain
import com.paan.safaadmin.domain.model.CategoryForm
import com.paan.safaadmin.domain.model.CategoryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: AdminApi,
) {
    private val _items = MutableStateFlow<List<CategoryRow>>(emptyList())
    val items: StateFlow<List<CategoryRow>> = _items.asStateFlow()

    suspend fun refresh(): Result<Unit> =
        apiCall { api.categories() }.map { list ->
            _items.value = list.map { it.toDomain() }.sortedBy { it.sortOrder }
            Unit
        }

    fun find(id: String): CategoryRow? = _items.value.firstOrNull { it.id == id }

    suspend fun save(id: String?, f: CategoryForm, image: ByteArray?): Result<Unit> {
        val form = Form()
            .text("name", f.name.trim()).text("description", f.description.trim())
            .text("sort_order", f.sortOrder.toString())
            .flag("is_active", f.isActive).flag("remove_image", f.removeImage)
            .image(image)
        if (id != null) form.text("_method", "PUT")
        val call = if (id == null) apiCall { api.createCategory(form.list()) } else apiCall { api.updateCategory(id, form.list()) }
        return call.map { Unit }.onSuccess { refresh() }
    }

    suspend fun delete(id: String): Result<Unit> =
        apiCallUnit { api.deleteCategory(id) }.onSuccess { _items.update { l -> l.filterNot { it.id == id } } }

    fun clear() { _items.value = emptyList() }
}