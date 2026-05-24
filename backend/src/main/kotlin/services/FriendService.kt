package app.deckbox.backend.services

import app.deckbox.backend.database.*
import app.deckbox.backend.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class FriendService {
    fun sendFriendRequest(senderId: String, receiverId: String): FriendRequest {
        val requestId = UUID.randomUUID().toString()

        return transaction {
            Friends.insert {
                it[id] = requestId
                it[Friends.senderId] = senderId
                it[Friends.receiverId] = receiverId
                it[status] = "pending"
                it[createdAt] = System.currentTimeMillis()
            }

            FriendRequest(requestId, senderId, receiverId, "pending")
        }
    }

    fun acceptFriendRequest(requestId: String): Boolean {
        return transaction {
            Friends.update({ Friends.id eq requestId }) {
                it[status] = "accepted"
            }
            true
        }
    }

    fun getPendingRequests(userId: String): List<FriendRequest> {
        return transaction {
            Friends.select { (Friends.receiverId eq userId) and (Friends.status eq "pending") }
                .map {
                    FriendRequest(
                        id = it[Friends.id],
                        senderId = it[Friends.senderId],
                        receiverId = it[Friends.receiverId],
                        status = it[Friends.status]
                    )
                }
        }
    }

    fun getFriends(userId: String): List<String> {
        return transaction {
            Friends.select {
                ((Friends.senderId eq userId) or (Friends.receiverId eq userId)) and (Friends.status eq "accepted")
            }.map {
                if (it[Friends.senderId] == userId) it[Friends.receiverId] else it[Friends.senderId]
            }
        }
    }
}