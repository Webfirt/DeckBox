package app.deckbox.backend.services

import app.deckbox.backend.database.*
import app.deckbox.backend.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TradeService {
    fun sendTrade(senderId: String, receiverId: String, offeredCards: List<String>, requestedCards: List<String>): Trade {
        val tradeId = UUID.randomUUID().toString()

        return transaction {
            Trades.insert {
                it[id] = tradeId
                it[Trades.senderId] = senderId
                it[Trades.receiverId] = receiverId
                it[Trades.offeredCards] = offeredCards.joinToString(",")
                it[Trades.requestedCards] = requestedCards.joinToString(",")
                it[status] = "pending"
                it[createdAt] = System.currentTimeMillis()
            }

            Trade(tradeId, senderId, receiverId, offeredCards, requestedCards, "pending")
        }
    }

    fun acceptTrade(tradeId: String): Boolean {
        return transaction {
            val tradeRow = Trades.select { Trades.id eq tradeId }.singleOrNull()
            if (tradeRow != null && tradeRow[Trades.status] == "pending") {
                val senderId = tradeRow[Trades.senderId]
                val receiverId = tradeRow[Trades.receiverId]
                val offeredCards = tradeRow[Trades.offeredCards].split(",").filter { it.isNotBlank() }
                val requestedCards = tradeRow[Trades.requestedCards].split(",").filter { it.isNotBlank() }

                transferCards(fromUserId = senderId, toUserId = receiverId, cardIds = offeredCards)
                transferCards(fromUserId = receiverId, toUserId = senderId, cardIds = requestedCards)

                Trades.update({ Trades.id eq tradeId }) {
                    it[status] = "accepted"
                }
                true
            } else false
        }
    }

    private fun transferCards(fromUserId: String, toUserId: String, cardIds: List<String>) {
        for (cardId in cardIds) {
            val fromRow = UserInventory.select {
                (UserInventory.userId eq fromUserId) and
                    (UserInventory.itemId eq cardId) and
                    (UserInventory.itemType eq "card")
            }.singleOrNull() ?: continue

            val currentQty = fromRow[UserInventory.quantity]
            if (currentQty <= 1) {
                UserInventory.deleteWhere {
                    (UserInventory.userId eq fromUserId) and
                        (UserInventory.itemId eq cardId) and
                        (UserInventory.itemType eq "card")
                }
            } else {
                UserInventory.update({
                    (UserInventory.userId eq fromUserId) and
                        (UserInventory.itemId eq cardId) and
                        (UserInventory.itemType eq "card")
                }) {
                    it[quantity] = currentQty - 1
                }
            }

            val toRow = UserInventory.select {
                (UserInventory.userId eq toUserId) and
                    (UserInventory.itemId eq cardId) and
                    (UserInventory.itemType eq "card")
            }.singleOrNull()

            if (toRow != null) {
                UserInventory.update({
                    (UserInventory.userId eq toUserId) and
                        (UserInventory.itemId eq cardId) and
                        (UserInventory.itemType eq "card")
                }) {
                    it[quantity] = toRow[UserInventory.quantity] + 1
                }
            } else {
                UserInventory.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[userId] = toUserId
                    it[itemId] = cardId
                    it[itemType] = "card"
                    it[quantity] = 1
                }
            }
        }
    }

    fun getPendingTrades(userId: String): List<Trade> {
        return transaction {
            Trades.select { (Trades.receiverId eq userId) and (Trades.status eq "pending") }
                .map {
                    Trade(
                        id = it[Trades.id],
                        senderId = it[Trades.senderId],
                        receiverId = it[Trades.receiverId],
                        offeredCards = it[Trades.offeredCards].split(","),
                        requestedCards = it[Trades.requestedCards].split(","),
                        status = it[Trades.status],
                    )
                }
        }
    }
}
