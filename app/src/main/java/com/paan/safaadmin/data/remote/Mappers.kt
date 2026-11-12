package com.paan.safaadmin.data.remote

import com.paan.safaadmin.domain.model.AdminUser
import com.paan.safaadmin.domain.model.HistoryEntry
import com.paan.safaadmin.domain.model.Order
import com.paan.safaadmin.domain.model.OrderLine
import com.paan.safaadmin.domain.model.OrderStatus
import com.paan.safaadmin.domain.model.CustomerRow
import com.paan.safaadmin.domain.model.Dashboard
import com.paan.safaadmin.domain.model.MenuItem
import com.paan.safaadmin.domain.model.ShopSettings
import com.paan.safaadmin.domain.model.CategoryRow
import com.paan.safaadmin.domain.model.CustomerDetail
import com.paan.safaadmin.domain.model.RecentOrder
import java.time.Instant
import java.time.OffsetDateTime

fun UserDto.toDomain() = AdminUser(id = id, name = name?.takeIf { it.isNotBlank() } ?: "Admin", phone = phone)

private val sweetnessLabels = mapOf(
    "regular" to "Regular Sweet",
    "mild" to "Mild Sweet",
    "low_gulkand" to "Low Gulkand",
    "extra_gulkand" to "Extra Gulkand",
)
private val supariLabels = mapOf(
    "fried" to "Fried Supari",
    "kacha" to "Kacha Supari",
)

private fun pretty(value: String?, labels: Map<String, String>, default: String): String {
    val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return default
    return labels[v.lowercase()] ?: v.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun OptionsDto?.summary() =
    "${pretty(this?.sweetness, sweetnessLabels, "Regular Sweet")} • ${pretty(this?.supari, supariLabels, "Fried Supari")}"

fun String.toOrderStatus(): OrderStatus = when (lowercase()) {
    "making" -> OrderStatus.MAKING
    "ready" -> OrderStatus.READY
    "completed" -> OrderStatus.COMPLETED
    "admin_cancelled", "cancelled" -> OrderStatus.CANCELLED
    else -> OrderStatus.PENDING
}

private fun String.paymentLabel() = when (lowercase()) {
    "paid" -> "Paid"
    "refunded" -> "Refunded"
    "failed" -> "Payment failed"
    else -> "Payment pending"
}

private fun parseTime(value: String): Long =
    runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(value).toEpochMilli() }
        .getOrDefault(0L)

fun OrderDto.toDomain() = Order(
    id = id,
    orderNumber = orderNumber,
    token = token,
    status = status.toOrderStatus(),
    paymentLabel = paymentStatus.paymentLabel(),
    total = total,
    comment = comment.orEmpty().trim(),
    customerName = customer?.name?.takeIf { it.isNotBlank() } ?: "Customer",
    customerPhone = customer?.phone.orEmpty(),
    items = items.map { OrderLine(it.name, it.quantity, it.options.summary(), it.unitPrice) },
    createdAt = parseTime(createdAt),
    history = history.map { HistoryEntry(it.status.toOrderStatus(), it.note?.takeIf { n -> n.isNotBlank() }, parseTime(it.createdAt)) },
    pickupQrPayload = pickupQr?.payload?.takeIf { it.isNotBlank() },
    completedVia = completedVia?.takeIf { it.isNotBlank() },
)

fun DashboardDto.toDomain() = Dashboard(
    todayOrders = todayOrders,
    pending = statusCounts["pending"] ?: 0,
    making = statusCounts["making"] ?: 0,
    ready = statusCounts["ready"] ?: 0,
    completed = statusCounts["completed"] ?: 0,
    cancelled = statusCounts["admin_cancelled"] ?: 0,
    todaySales = todaySales,
    totalSales = totalSales,
    totalCustomers = totalCustomers,
)

fun ProductDto.toDomain() = MenuItem(
    id = id, name = name, description = description.orEmpty(), price = price,
    categoryId = categoryId.ifEmpty { category?.id.orEmpty() }, category = category?.name.orEmpty(),
    imageUrl = thumbnailUrl ?: imageUrl, fullImageUrl = imageUrl,
    isAvailable = isAvailable, isFeatured = isFeatured, tag = tag?.takeIf { it.isNotBlank() },
    prepMinutes = prepMinutes, ingredients = ingredients, sortOrder = sortOrder,
)

fun CategoryRowDto.toDomain() = CategoryRow(
    id = id, name = name, description = description.orEmpty(), imageUrl = thumbnailUrl ?: imageUrl,
    sortOrder = sortOrder, isActive = isActive, productsCount = productsCount,
)

fun SettingsDto.toDomain() = ShopSettings(
    name = shopName,
    address = shopAddress.orEmpty(),
    phone = shopPhone.orEmpty(),
    openingTime = openingTime.orEmpty().take(5),   // "09:00:00" -> "09:00"
    closingTime = closingTime.orEmpty().take(5),
    pickupMessage = pickupMessage.orEmpty(),
    isOpen = shopStatus.equals("open", ignoreCase = true),
)

fun ShopSettings.toDto() = SettingsDto(
    shopName = name.trim(),
    shopAddress = address.trim(),
    shopPhone = phone.trim(),
    openingTime = openingTime.trim(),
    closingTime = closingTime.trim(),
    pickupMessage = pickupMessage.trim(),
    shopStatus = if (isOpen) "open" else "closed",
)

fun CustomerRowDto.toDomain() = CustomerRow(
    id = id,
    name = name?.takeIf { it.isNotBlank() } ?: "Customer",
    phone = phone,
    isActive = isActive,
    ordersCount = ordersCount,
    joinedAt = parseTime(createdAt),
    email = email.orEmpty(),
    blockedReason = blockedReason?.takeIf { it.isNotBlank() },
)

fun CustomerDetailDto.toDomain() = CustomerDetail(
    customer = CustomerRow(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: "Customer",
        phone = phone,
        isActive = isActive,
        ordersCount = ordersCount,
        joinedAt = parseTime(createdAt),
        email = email.orEmpty(),
        blockedReason = blockedReason?.takeIf { it.isNotBlank() },
    ),
    recentOrders = recentOrders.map {
        RecentOrder(it.id, it.orderNumber, it.status.toOrderStatus(), it.total, parseTime(it.createdAt))
    },
    totalSpent = totalSpent,
)