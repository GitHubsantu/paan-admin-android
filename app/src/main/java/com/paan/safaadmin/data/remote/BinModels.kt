package com.paan.safaadmin.data.remote

import com.paan.safaadmin.domain.model.DeletedCustomer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

@Serializable
data class DeletedCustomerDto(
    @Serializable(with = FlexStringSerializer::class) val id: String,
    val name: String? = null,
    val phone: String = "",
    val email: String? = null,
    @Serializable(with = FlexIntSerializer::class) @SerialName("orders_count") val ordersCount: Int = 0,
    @SerialName("deleted_at") val deletedAt: String = "",
)

private fun parseDeletedAt(value: String): Long =
    runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(value).toEpochMilli() }
        .getOrDefault(0L)

fun DeletedCustomerDto.toDomain() = DeletedCustomer(
    id = id,
    name = name?.takeIf { it.isNotBlank() } ?: "Customer",
    phone = phone,
    email = email.orEmpty(),
    ordersCount = ordersCount,
    deletedAt = parseDeletedAt(deletedAt),
)