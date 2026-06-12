package com.georacing.georacing.domain.products

interface ProductsRepository {
    suspend fun getAllProducts(): List<Product>
    suspend fun updateProductStock(productId: String, newStock: Int)
    suspend fun upsertProduct(product: Product)
}
