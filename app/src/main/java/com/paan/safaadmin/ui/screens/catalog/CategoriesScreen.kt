package com.paan.safaadmin.ui.screens.catalog

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.paan.safaadmin.core.util.compressImage
import com.paan.safaadmin.data.repository.CategoryRepository
import com.paan.safaadmin.data.repository.LoadState
import com.paan.safaadmin.domain.model.CategoryForm
import com.paan.safaadmin.domain.model.CategoryRow
import com.paan.safaadmin.navigation.Route
import com.paan.safaadmin.ui.components.*
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------- List ----------------

data class CategoriesUiState(
    val items: List<CategoryRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
) : ViewModel() {
    private val load = MutableStateFlow(LoadState())

    val state: StateFlow<CategoriesUiState> = combine(repo.items, load) { items, l ->
        CategoriesUiState(items, isLoading = !l.loaded && items.isEmpty(), error = if (items.isEmpty()) l.error else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { load.value = LoadState(loaded = true, error = repo.refresh().exceptionOrNull()?.message) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = PaanColors.ForestGreen,
                contentColor = PaanColors.Cream,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add category") },
            )
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PaanColors.ForestGreen) }
                state.error != null -> EmptyState("Couldn't load categories", state.error!!, actionLabel = "Try again", onAction = viewModel::refresh)
                state.items.isEmpty() -> EmptyState("No categories yet", "Tap \"Add category\" to create the first one.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    items(state.items, key = { it.id }, contentType = { "category" }) { c ->
                        Card(
                            Modifier.fillMaxWidth().clickable { onEdit(c.id) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = PaanColors.CardWhite),
                            border = BorderStroke(1.dp, PaanColors.RoyalGold.copy(alpha = 0.25f)),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(PaanColors.ForestGreen.copy(alpha = 0.08f))) {
                                    if (c.imageUrl != null) {
                                        AsyncImage(c.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${c.productsCount} ${if (c.productsCount == 1) "product" else "products"}",
                                        style = MaterialTheme.typography.bodySmall, color = PaanColors.MutedBrown,
                                    )
                                }
                                Text(
                                    if (c.isActive) "Active" else "Hidden",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (c.isActive) PaanColors.FreshGreen else PaanColors.Maroon,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Form ----------------

data class CategoryFormState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val description: String = "",
    val sortOrder: String = "0",
    val isActive: Boolean = true,
    val existingImage: String? = null,
    val pickedUri: Uri? = null,
    val removeImage: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val loadError: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repo: CategoryRepository,
) : ViewModel() {
    private val categoryId: String? = savedStateHandle[Route.CategoryForm.ARG_ID]
    private val _state = MutableStateFlow(CategoryFormState(isEdit = categoryId != null))
    val state: StateFlow<CategoryFormState> = _state.asStateFlow()
    private var imageBytes: ByteArray? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            if (categoryId != null && repo.find(categoryId) == null) {
                repo.refresh().onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.message) }; return@launch }
            }
            val c = categoryId?.let { repo.find(it) }
            if (categoryId != null && c == null) {
                _state.update { it.copy(isLoading = false, loadError = "Category not found") }
                return@launch
            }
            _state.update { s ->
                if (c == null) s.copy(isLoading = false)
                else s.copy(
                    isLoading = false, name = c.name, description = c.description,
                    sortOrder = c.sortOrder.toString(), isActive = c.isActive, existingImage = c.imageUrl,
                )
            }
        }
    }

    fun edit(change: CategoryFormState.() -> CategoryFormState) = _state.update { it.change().copy(error = null) }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val bytes = compressImage(context, uri)
            if (bytes == null) _state.update { it.copy(error = "Couldn't read that photo") }
            else {
                imageBytes = bytes
                _state.update { it.copy(pickedUri = uri, removeImage = false, error = null) }
            }
        }
    }

    fun removePhoto() {
        imageBytes = null
        _state.update { it.copy(pickedUri = null, existingImage = null, removeImage = true) }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        val error = when {
            s.name.trim().length < 2 -> "Enter the category name"
            s.sortOrder.toIntOrNull() == null -> "Sort order must be a number"
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val form = CategoryForm(s.name, s.description, s.sortOrder.toInt(), s.isActive, s.removeImage)
            repo.save(categoryId, form, imageBytes)
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save") } }
        }
    }

    fun delete() {
        val id = categoryId ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            repo.delete(id)
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                // e.g. "This category still has 3 products. Move or delete them first."
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't delete") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(s.done) { if (s.done) onDone() }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = PaanColors.Cream,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (s.isEdit) "Edit category" else "Add category", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
        bottomBar = {
            if (!s.isLoading && s.loadError == null) {
                Surface(color = PaanColors.CardWhite, shadowElevation = 12.dp) {
                    PrimaryPaanButton(
                        "Save", viewModel::save, isLoading = s.saving,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PaanColors.ForestGreen) }
                s.loadError != null -> EmptyState("Couldn't load the form", s.loadError!!, actionLabel = "Try again", onAction = viewModel::load)
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PhotoPicker(s.pickedUri ?: s.existingImage, viewModel::onImagePicked, viewModel::removePhoto)
                    AppField("Name", s.name, { v -> viewModel.edit { copy(name = v.take(60)) } })
                    AppField("Description (optional)", s.description, { v -> viewModel.edit { copy(description = v.take(255)) } }, singleLine = false)
                    AppField("Sort order", s.sortOrder, { v -> viewModel.edit { copy(sortOrder = v.filter(Char::isDigit).take(4)) } },
                        keyboardType = KeyboardType.Number)
                    SwitchRow("Visible to customers", s.isActive) { v -> viewModel.edit { copy(isActive = v) } }
                    s.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = PaanColors.Error) }
                    if (s.isEdit) {
                        OutlinedButton(
                            onClick = { confirmDelete = true }, enabled = !s.saving, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
                        ) { Text("Delete this category") }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this category?") },
            text = { Text("Categories that still have products can't be deleted.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete", color = PaanColors.Maroon) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
    }
}