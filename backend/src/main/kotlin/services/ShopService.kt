package app.deckbox.backend.services

import app.deckbox.backend.database.*
import app.deckbox.backend.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ShopService {
    fun getShopItems(): List<ShopItem> {
        return listOf(
            ShopItem("gold_pack_100", "Gold Pack (100)", "Get 100 gold coins", 5, "gold"),
            ShopItem("gold_pack_500", "Gold Pack (500)", "Get 500 gold coins", 20, "gold"),
            ShopItem("premium_sleeve", "Premium Sleeve", "Show off rare art on your deck sleeve", 150, "premium"),
            ShopItem("booster_pack", "Booster Pack", "Open a pack of 10 random cards", 10, "card_pack")
        )
    }

    fun buyItem(userId: String, itemId: String): Boolean {
        return transaction {
            val user = Users.select { Users.id eq userId }.singleOrNull()
            val item = getShopItems().find { it.id == itemId }

            if (user != null && item != null && user[Users.gold] >= item.price) {
                Users.update({ Users.id eq userId }) {
                    it[gold] = user[Users.gold] - item.price
                }

                // Add item to inventory
                UserInventory.insert {
                    it[UserInventory.id] = java.util.UUID.randomUUID().toString()
                    it[UserInventory.userId] = userId
                    it[UserInventory.itemId] = itemId
                    it[UserInventory.itemType] = if (item.type == "card_pack") "card_pack" else "item"
                    it[quantity] = 1
                }

                true
            } else false
        }
    }
}