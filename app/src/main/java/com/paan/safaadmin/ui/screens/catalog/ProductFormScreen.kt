package com.paan.safaadmin.ui.screens.catalog

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.paan.safaadmin.core.util.compressImage
import com.paan.safaadmin.data.repository.CategoryRepository
import com.paan.safaadmin.data.repository.MenuRepository
import com.paan.safaadmin.domain.model.CategoryRow
import com.paan.safaadmin.domain.model.ProductForm
import com.paan.safaadmin.navigation.Route
import com.paan.safaadmin.ui.components.*
import com.paan.safaadmin.ui.theme.Dimens
import com.paan.safaadmin.ui.theme.PaanColors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val categoryId: String = "",
    val price: String = "",
    val description: String = "",
    val tag: String = "",
    val prepMinutes: String = "3",
    val ingredients: List<String> = emptyList(),
    val ingredientDraft: String = "",
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false,
    val sortOrder: String = "0",
    val categories: List<CategoryRow> = emptyList(),
    val existingImage: String? = null,
    val pickedUri: Uri? = null,
    val removeImage: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val loadError: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val menu: MenuRepository,
    private val categories: CategoryRepository,
) : ViewModel() {

    private val productId: String? = savedStateHandle[Route.ProductForm.ARG_ID]
    private val _state = MutableStateFlow(ProductFormState(isEdit = productId != null))
    val state: StateFlow<ProductFormState> = _state.asStateFlow()
    private var imageBytes: ByteArray? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            categories.refresh().onFailure { e ->
                _state.update { it.copy(isLoading = false, loadError = e.message) }
                return@launch
            }
            if (productId != null && menu.find(productId) == null) {
                menu.refresh().onFailure { e ->
                    _state.update { it.copy(isLoading = false, loadError = e.message) }
                    return@launch
                }
            }
            val item = productId?.let { menu.find(it) }
            if (productId != null && item == null) {
                _state.update { it.copy(isLoading = false, loadError = "Product not found") }
                return@launch
            }
            val list = categories.items.value
            _state.update { s ->
                val base = s.copy(isLoading = false, categories = list)
                if (item == null) base.copy(categoryId = list.firstOrNull()?.id.orEmpty())
                else base.copy(
                    name = item.name, categoryId = item.categoryId, price = item.price.toString(),
                    description = item.description, tag = item.tag.orEmpty(), prepMinutes = item.prepMinutes.toString(),
                    ingredients = item.ingredients, isAvailable = item.isAvailable, isFeatured = item.isFeatured,
                    sortOrder = item.sortOrder.toString(), existingImage = item.fullImageUrl ?: item.imageUrl,
                )
            }
        }
    }

    fun edit(change: ProductFormState.() -> ProductFormState) = _state.update { it.change().copy(error = null) }

    fun addIngredient() = _state.update {
        val v = it.ingredientDraft.trim()
        if (v.isEmpty() || v in it.ingredients || it.ingredients.size >= 10) it.copy(ingredientDraft = "")
        else it.copy(ingredients = it.ingredients + v.take(40), ingredientDraft = "")
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val bytes = compressImage(context, uri)
            if (bytes == null) {
                _state.update { it.copy(error = "Couldn't read that photo") }
            } else {
                imageBytes = bytes
                _state.update { it.copy(pickedUri = uri, removeImage = false, error = null) }
            }
        }
    }

    fun removePhoto() {
        imageBytes = null
        _state.update { it.copy(pickedUri = null, existingImage = null, removeImage = true) }
    }

    private fun validate(s: ProductFormState): String? = when {
        s.name.trim().length < 2 -> "Enter the paan name"
        s.categoryId.isEmpty() -> "Choose a category (create one first if the list is empty)"
        s.price.toIntOrNull() == null -> "Enter the price in rupees"
        (s.prepMinutes.toIntOrNull() ?: 0) !in 1..60 -> "Preparation time must be 1 to 60 minutes"
        s.sortOrder.toIntOrNull() == null -> "Sort order must be a number"
        else -> null
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        validate(s)?.let { err -> _state.update { it.copy(error = err) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val form = ProductForm(
                name = s.name, categoryId = s.categoryId, price = s.price.toInt(), description = s.description,
                tag = s.tag, prepMinutes = s.prepMinutes.toInt(), ingredients = s.ingredients,
                isAvailable = s.isAvailable, isFeatured = s.isFeatured, sortOrder = s.sortOrder.toInt(),
                removeImage = s.removeImage,
            )
            menu.save(productId, form, imageBytes)
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save") } }
        }
    }

    fun delete() {
        val id = productId ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            menu.delete(id)
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't delete") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel(),
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
                title = { Text(if (s.isEdit) "Edit paan" else "Add paan", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaanColors.Cream),
            )
        },
        bottomBar = {
            if (!s.isLoading && s.loadError == null) {
                Surface(color = PaanColors.CardWhite, shadowElevation = 12.dp) {
                    PrimaryPaanButton(
                        text = "Save",
                        onClick = viewModel::save,
                        isLoading = s.saving,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        PaanBackground(Modifier.padding(padding)) {
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PaanColors.ForestGreen)
                }
                s.loadError != null -> EmptyState(
                    title = "Couldn't load the form", message = s.loadError!!,
                    actionLabel = "Try again", onAction = viewModel::load,
                )
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PhotoPicker(
                        model = s.pickedUri ?: s.existingImage,
                        onPick = viewModel::onImagePicked,
                        onRemove = viewModel::removePhoto,
                    )
                    AppField("Name", s.name, { v -> viewModel.edit { copy(name = v.take(80)) } })

                    Text("Category", style = MaterialTheme.typography.titleMedium)
                    if (s.categories.isEmpty()) {
                        Text("No categories yet. Create one first.", style = MaterialTheme.typography.bodySmall, color = PaanColors.Maroon)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        s.categories.forEach { c ->
                            FilterChip(
                                selected = s.categoryId == c.id,
                                onClick = { viewModel.edit { copy(categoryId = c.id) } },
                                label = { Text(c.name) },
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppField("Price (₹)", s.price, { v -> viewModel.edit { copy(price = v.filter(Char::isDigit).take(6)) } },
                            Modifier.weight(1f), KeyboardType.Number)
                        AppField("Prep (min)", s.prepMinutes, { v -> viewModel.edit { copy(prepMinutes = v.filter(Char::isDigit).take(2)) } },
                            Modifier.weight(1f), KeyboardType.Number)
                    }
                    AppField("Description", s.description, { v -> viewModel.edit { copy(description = v.take(500)) } }, singleLine = false)
                    AppField("Label (e.g. Chandi Vark)", s.tag, { v -> viewModel.edit { copy(tag = v.take(30)) } })

                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppField("Add ingredient", s.ingredientDraft, { v -> viewModel.edit { copy(ingredientDraft = v.take(40)) } },
                            Modifier.weight(1f))
                        TextButton(onClick = viewModel::addIngredient) { Text("Add", color = PaanColors.ForestGreen) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.ingredients.forEach { ing ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.edit { copy(ingredients = ingredients - ing) } },
                                label = { Text(ing) },
                                trailingIcon = { Icon(Icons.Filled.Close, "Remove $ing", Modifier.size(16.dp)) },
                            )
                        }
                    }

                    SwitchRow("Available to order", s.isAvailable) { v -> viewModel.edit { copy(isAvailable = v) } }
                    SwitchRow("Featured on Home", s.isFeatured) { v -> viewModel.edit { copy(isFeatured = v) } }
                    AppField("Sort order", s.sortOrder, { v -> viewModel.edit { copy(sortOrder = v.filter(Char::isDigit).take(4)) } },
                        keyboardType = KeyboardType.Number)

                    s.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = PaanColors.Error) }

                    if (s.isEdit) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            enabled = !s.saving,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PaanColors.Maroon),
                        ) { Text("Delete this paan") }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this paan?") },
            text = { Text("It disappears from the menu. Old orders keep their details.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete", color = PaanColors.Maroon) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = PaanColors.ForestGreen) } },
            containerColor = PaanColors.Cream,
        )
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PaanColors.ForestGreen, checkedThumbColor = PaanColors.Cream),
        )
    }
}