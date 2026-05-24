package app.deckbox.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val gold: Int = 100,
    val experience: Int = 0,
    val premiumPassActive: Boolean = false,
    val dailyRewardClaimed: Boolean = false
)

@Serializable
data class Card(
    val id: String,
    val name: String,
    val type: String,
    val rarity: String
)

@Serializable
data class Deck(
    val id: String,
    val userId: String,
    val name: String,
    val cards: List<String> // Card IDs
)

@Serializable
data class Battle(
    val id: String,
    val player1Id: String,
    val player2Id: String,
    val status: String, // "waiting", "in_progress", "finished"
    val winnerId: String? = null
)

@Serializable
data class Trade(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val offeredCards: List<String>,
    val requestedCards: List<String>,
    val status: String // "pending", "accepted", "declined"
)

@Serializable
data class FriendRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String // "pending", "accepted", "declined"
)

@Serializable
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val type: String // "gold", "premium", "card_pack"
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val score: Int
)