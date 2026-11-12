package com.paan.safaadmin.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

/** Reads 40, 40.00 and "40.00" alike. */
object FlexIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val text = (element as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content ?: return 0
        return text.toDoubleOrNull()?.roundToInt() ?: 0
    }
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/** Reads ids whether the server sends 7 or "7". */
object FlexStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexString", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content ?: ""
    }
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

@Serializable
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null,
    val meta: PageMeta? = null,
    val errors: Map<String, List<String>>? = null,
    val code: String? = null,
)

@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    val total: Int = 0,
)

// ---- Auth ----

@Serializable
data class AdminLoginRequest(val phone: String, val password: String)

@Serializable
data class UserDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String? = null,
    val phone: String = "",
)

@Serializable
data class AdminAuthDto(val token: String, val user: UserDto)

// ---- Orders ----

@Serializable
data class StatusRequest(val status: String, val note: String? = null)

@Serializable
data class CancelRequest(val reason: String)

@Serializable
data class CustomerDto(val name: String? = null, val phone: String = "")

@Serializable
data class OptionsDto(val sweetness: String? = null, val supari: String? = null)

@Serializable
data class OrderItemDto(
    val name: String,
    val quantity: Int = 1,
    @Serializable(with = FlexIntSerializer::class) @SerialName("unit_price") val unitPrice: Int = 0,
    val options: OptionsDto? = null,
)

@Serializable
data class HistoryDto(
    val status: String,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
@Serializable
data class PickupQrDto(val payload: String = "")

@Serializable
data class OrderDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    @SerialName("order_number") val orderNumber: String,
    @Serializable(with = FlexIntSerializer::class) val token: Int = 0,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String = "pending",
    @Serializable(with = FlexIntSerializer::class) val total: Int = 0,
    val comment: String? = null,
    val customer: CustomerDto? = null,
    val items: List<OrderItemDto> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    val history: List<HistoryDto> = emptyList(),
    @SerialName("pickup_qr") val pickupQr: PickupQrDto? = null,
    @SerialName("completed_via") val completedVia: String? = null,
)

// ---- Dashboard / menu / shop / customers ----

@Serializable
data class DashboardDto(
    @Serializable(with = FlexIntSerializer::class) @SerialName("today_orders") val todayOrders: Int = 0,
    @SerialName("status_counts") val statusCounts: Map<String, Int> = emptyMap(),
    @Serializable(with = FlexIntSerializer::class) @SerialName("today_sales") val todaySales: Int = 0,
    @Serializable(with = FlexIntSerializer::class) @SerialName("total_sales") val totalSales: Int = 0,
    @Serializable(with = FlexIntSerializer::class) @SerialName("total_customers") val totalCustomers: Int = 0,
)

@Serializable
data class CategoryDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String,
)

@Serializable
data class ProductDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String,
    val description: String? = null,
    @Serializable(with = FlexIntSerializer::class) val price: Int = 0,
    val category: CategoryDto? = null,
    @Serializable(with = FlexStringSerializer::class) @SerialName("category_id") val categoryId: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    val tag: String? = null,
    @SerialName("prep_minutes") val prepMinutes: Int = 3,
    val ingredients: List<String> = emptyList(),
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class CategoryRowDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @Serializable(with = FlexIntSerializer::class) @SerialName("products_count") val productsCount: Int = 0,
)

@Serializable
data class AvailabilityRequest(@SerialName("is_available") val isAvailable: Boolean)

@Serializable
data class SettingsDto(
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("shop_address") val shopAddress: String? = null,
    @SerialName("shop_phone") val shopPhone: String? = null,
    @SerialName("opening_time") val openingTime: String? = null,
    @SerialName("closing_time") val closingTime: String? = null,
    @SerialName("pickup_message") val pickupMessage: String? = null,
    @SerialName("shop_status") val shopStatus: String = "open",
)

@Serializable
data class CustomerRowDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String? = null,
    val phone: String = "",
    val email: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("blocked_reason") val blockedReason: String? = null,
    @Serializable(with = FlexIntSerializer::class) @SerialName("orders_count") val ordersCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class RecentOrderDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    @SerialName("order_number") val orderNumber: String = "",
    val status: String = "pending",
    @Serializable(with = FlexIntSerializer::class) val total: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CustomerDetailDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String? = null,
    val phone: String = "",
    val email: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("blocked_reason") val blockedReason: String? = null,
    @Serializable(with = FlexIntSerializer::class) @SerialName("orders_count") val ordersCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("recent_orders") val recentOrders: List<RecentOrderDto> = emptyList(),
    @Serializable(with = FlexIntSerializer::class) @SerialName("total_spent") val totalSpent: Int = 0,
)

@Serializable
data class CustomerRequest(val name: String, val phone: String, val email: String)

@Serializable
data class BlockRequest(
    @SerialName("is_active") val isActive: Boolean,
    val reason: String? = null,
)
@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String,
    val app: String,
)