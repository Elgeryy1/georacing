package com.georacing.georacing.domain.products

/**
 * Modelo de dominio de Producto.
 * Compatible con ambos esquemas:
 *   - Panel Metropolis: id, name, price, category, emoji, in_stock
 *   - Legacy app:       product_id, name, description, price, stock, category, image_url, is_active
 */
data class Product(
    val productId: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val stock: Int = 0,
    val category: String,
    val imageUrl: String? = null,
    val emoji: String? = null,
    val isActive: Boolean = true
)
