package com.paan.safaadmin.domain.model

data class AdminUser(val id: String, val name: String, val phone: String)

enum class OrderStatus(val label: String, val api: String) {
    PENDING("New", "pending"),
    MAKING("Preparing", "making"),
    READY("Ready", "ready"),
    COMPLETED("Collected", "completed"),
    CANCELLED("Cancelled", "admin_cancelled");

    val isActive: Boolean get() = this == PENDING || this == MAKING || this == READY

    /** The next step the shop can take, or null when the order is finished. */
    val next: OrderStatus?
        get() = when (this) {
            PENDING -> MAKING
            MAKING -> READY
            READY -> COMPLETED
            else -> null
        }

    /** Text for the button that moves the order forward. */
    val actionLabel: String?
        get() = when (this) {
            PENDING -> "Start making"
            MAKING -> "Mark ready"
            READY -> "Mark collected"
            else -> null
        }
}

data class OrderLine(
    val name: String,
    val quantity: Int,
    val options: String,     // "Mild Sweet • Kacha Supari"
    val unitPrice: Int,
)

data class HistoryEntry(val status: OrderStatus, val note: String?, val at: Long)

data class Order(
    val id: String,
    val orderNumber: String,
    val token: Int,
    val status: OrderStatus,
    val paymentLabel: String,
    val total: Int,
    val comment: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<OrderLine>,
    val createdAt: Long,
    val history: List<HistoryEntry>,
    /** Only present while the order is ready. Never log or show this text. */
    val pickupQrPayload: String? = null,
    val completedVia: String? = null,   // "qr" | "admin"
) {
    val itemCount: Int get() = items.sumOf { it.quantity }

    val completedViaLabel: String?
        get() = when (completedVia) {
            "qr" -> "Collected by QR scan"
            "admin" -> "Marked collected by staff"
            else -> null
        }
}

data class Dashboard(
    val todayOrders: Int,
    val pending: Int,
    val making: Int,
    val ready: Int,
    val completed: Int,
    val cancelled: Int,
    val todaySales: Int,
    val totalSales: Int,
    val totalCustomers: Int,
)

data class MenuItem(
    val id: String, val name: String, val description: String, val price: Int,
    val categoryId: String, val category: String,
    val imageUrl: String?, val fullImageUrl: String?,
    val isAvailable: Boolean, val isFeatured: Boolean, val tag: String?,
    val prepMinutes: Int, val ingredients: List<String>, val sortOrder: Int,
)

data class CategoryRow(
    val id: String, val name: String, val description: String, val imageUrl: String?,
    val sortOrder: Int, val isActive: Boolean, val productsCount: Int,
)

data class ProductForm(
    val name: String, val categoryId: String, val price: Int, val description: String, val tag: String,
    val prepMinutes: Int, val ingredients: List<String>, val isAvailable: Boolean, val isFeatured: Boolean,
    val sortOrder: Int, val removeImage: Boolean,
)

data class CategoryForm(
    val name: String, val description: String, val sortOrder: Int, val isActive: Boolean, val removeImage: Boolean,
)

data class ShopSettings(
    val name: String,
    val address: String,
    val phone: String,
    val openingTime: String,
    val closingTime: String,
    val pickupMessage: String,
    val isOpen: Boolean,
)

data class CustomerRow(
    val id: String,
    val name: String,
    val phone: String,
    val isActive: Boolean,
    val ordersCount: Int,
    val joinedAt: Long,
    val email: String = "",
    val blockedReason: String? = null,
)

data class RecentOrder(
    val id: String,
    val orderNumber: String,
    val status: OrderStatus,
    val total: Int,
    val createdAt: Long,
)

data class CustomerDetail(
    val customer: CustomerRow,
    val recentOrders: List<RecentOrder>,
    val totalSpent: Int,
)

data class CustomerForm(val name: String, val phone: String, val email: String)