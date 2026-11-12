package com.paan.safaadmin.domain.model

data class DeletedCustomer(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val ordersCount: Int,
    val deletedAt: Long,
)