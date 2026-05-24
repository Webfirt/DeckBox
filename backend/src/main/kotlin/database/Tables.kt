package app.deckbox.backend.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 50).primaryKey()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val gold = integer("gold").default(100)
    val experience = integer("experience").default(0)
    val premiumPassActive = bool("premium_pass_active").default(false)
    val dailyRewardClaimed = bool("daily_reward_claimed").default(false)
    val lastLogin = long("last_login").nullable()
    val lastDailyClaimDate = varchar("last_daily_claim_date", 10).nullable() // "YYYY-MM-DD"
}

object Cards : Table() {
    val id = varchar("id", 50).primaryKey()
    val name = varchar("name", 100)
    val type = varchar("type", 50)
    val rarity = varchar("rarity", 50)
}

object Decks : Table() {
    val id = varchar("id", 50).primaryKey()
    val userId = varchar("user_id", 50).references(Users.id)
    val name = varchar("name", 100)
    val cards = text("cards") // JSON array of card IDs
}

object Battles : Table() {
    val id = varchar("id", 50).primaryKey()
    val player1Id = varchar("player1_id", 50).references(Users.id)
    val player2Id = varchar("player2_id", 50).references(Users.id)
    val status = varchar("status", 20)
    val winnerId = varchar("winner_id", 50).nullable()
    val createdAt = long("created_at")
}

object Trades : Table() {
    val id = varchar("id", 50).primaryKey()
    val senderId = varchar("sender_id", 50).references(Users.id)
    val receiverId = varchar("receiver_id", 50).references(Users.id)
    val offeredCards = text("offered_cards") // JSON
    val requestedCards = text("requested_cards") // JSON
    val status = varchar("status", 20)
    val createdAt = long("created_at")
}

object Friends : Table() {
    val id = varchar("id", 50).primaryKey()
    val senderId = varchar("sender_id", 50).references(Users.id)
    val receiverId = varchar("receiver_id", 50).references(Users.id)
    val status = varchar("status", 20)
    val createdAt = long("created_at")
}

object ShopItems : Table() {
    val id = varchar("id", 50).primaryKey()
    val name = varchar("name", 100)
    val description = text("description")
    val price = integer("price")
    val type = varchar("type", 50)
}

object UserInventory : Table() {
    val id = varchar("id", 50).primaryKey()
    val userId = varchar("user_id", 50).references(Users.id)
    val itemId = varchar("item_id", 50) // References Cards or ShopItems
    val itemType = varchar("item_type", 20) // "card", "item"
    val quantity = integer("quantity").default(1)
}

object PasswordResets : Table() {
    val email = varchar("email", 100)
    val code = varchar("code", 10)
    val expiresAt = long("expires_at")
}