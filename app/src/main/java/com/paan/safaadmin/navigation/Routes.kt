package com.paan.safaadmin.navigation

sealed class Route(val path: String) {
    data object Login : Route("login")
    data object Queue : Route("queue")
    data object Detail : Route("order/{id}") {
        const val ARG_ID = "id"
        fun create(id: String) = "order/$id"
    }
    data object Dashboard : Route("dashboard")
    data object Menu : Route("menu")
    data object Shop : Route("shop")
    data object Customers : Route("customers")
    data object CustomerDetail : Route("customer/{id}") {
        const val ARG_ID = "id"
        fun create(id: String) = "customer/$id"
    }
    data object CustomerForm : Route("customer-form?id={id}") {
        const val ARG_ID = "id"
        fun create(id: String? = null) = if (id == null) "customer-form" else "customer-form?id=$id"
    }

    data object CustomerBin : Route("customers/bin")

    data object CategoryList : Route("categories")
    data object CategoryForm : Route("category-form?id={id}") {
        const val ARG_ID = "id"
        fun create(id: String? = null) = if (id == null) "category-form" else "category-form?id=$id"
    }
    data object ProductForm : Route("product-form?id={id}") {
        const val ARG_ID = "id"
        fun create(id: String? = null) = if (id == null) "product-form" else "product-form?id=$id"
    }

    data object AlertSetup : Route("alert-setup")
}