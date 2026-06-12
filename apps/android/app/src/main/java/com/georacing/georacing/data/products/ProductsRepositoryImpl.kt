package com.georacing.georacing.data.products

import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.products.Product
import com.georacing.georacing.domain.products.ProductsRepository

class ProductsRepositoryImpl : ProductsRepository {

    private val api = FirestoreLikeClient.api

    override suspend fun getAllProducts(): List<Product> {
        // Lee de la tabla "products" — compatible con ambos esquemas:
        //   Panel Metropolis: id, name, price, category, emoji, in_stock
        //   Legacy:           product_id, name, description, price, stock, category, image_url, is_active
        val rawList = api.read("products")
        return rawList.mapNotNull { map ->
            try {
                val id = map["product_id"] as? String
                    ?: map["id"]?.toString()
                    ?: return@mapNotNull null

                val inStock = when (val v = map["in_stock"]) {
                    is Boolean -> v
                    is Number -> v.toInt() == 1
                    else -> map["is_active"] as? Boolean ?: true
                }

                Product(
                    productId = id,
                    name = map["name"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                    stock = (map["stock"] as? Number)?.toInt() ?: if (inStock) 99 else 0,
                    category = map["category"] as? String ?: "General",
                    imageUrl = map["image_url"] as? String,
                    emoji = map["emoji"] as? String,
                    isActive = inStock
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun updateProductStock(productId: String, newStock: Int) {
        // I'll try to get the internal ID first to be safe.
        // If the table doesn't exist, api.get throws 500. We must catch that.
        val existing = try {
            api.get(
                com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(
                    table = "products",
                    where = mapOf("product_id" to productId)
                )
            ).firstOrNull()
        } catch (e: Exception) {
            // Table might not exist or connection error. Assume not found.
            null
        }

        val internalId = existing?.get("id")
        
        val data = mutableMapOf<String, Any?>(
            "product_id" to productId,
            "stock" to newStock
        )
        
        if (internalId != null) {
            data["id"] = internalId
        }

        api.upsert(
            com.georacing.georacing.data.firestorelike.FirestoreLikeApi.UpsertRequest(
                table = "products",
                data = data
            )
        )
    }

    override suspend fun upsertProduct(product: Product) {
        // Escribe en formato compatible con Panel Metropolis
        val data = mutableMapOf<String, Any?>(
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "category" to product.category,
            "emoji" to (product.emoji ?: "📦"),
            "in_stock" to if (product.isActive) 1 else 0
        )

        // Buscar registro existente por product_id o id
        val existing = try {
            api.get(
                com.georacing.georacing.data.firestorelike.FirestoreLikeApi.GetRequest(
                    table = "products",
                    where = mapOf("id" to product.productId)
                )
            ).firstOrNull()
        } catch (e: Exception) {
            null
        }

        val internalId = existing?.get("id")
        if (internalId != null) {
            data["id"] = internalId
        }
        
        api.upsert(
            com.georacing.georacing.data.firestorelike.FirestoreLikeApi.UpsertRequest(
                table = "products",
                data = data
            )
        )
    }
}
