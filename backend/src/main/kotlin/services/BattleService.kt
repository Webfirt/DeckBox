package app.deckbox.backend.services

import app.deckbox.backend.database.*
import app.deckbox.backend.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BattleService {
    private val waitingQueue = mutableListOf<String>()

    fun matchmake(playerId: String): Battle? {
        synchronized(waitingQueue) {
            val opponentIdx = waitingQueue.indexOfFirst { it != playerId }
            return if (opponentIdx >= 0) {
                val opponentId = waitingQueue.removeAt(opponentIdx)
                createBattle(playerId, opponentId)
            } else {
                if (playerId !in waitingQueue) waitingQueue.add(playerId)
                null
            }
        }
    }

    fun createBattle(player1Id: String, player2Id: String): Battle {
        val battleId = UUID.randomUUID().toString()

        return transaction {
            Battles.insert {
                it[id] = battleId
                it[Battles.player1Id] = player1Id
                it[Battles.player2Id] = player2Id
                it[status] = "in_progress"
                it[createdAt] = System.currentTimeMillis()
            }

            Battle(battleId, player1Id, player2Id, "in_progress")
        }
    }

    fun getBattle(battleId: String): Battle? {
        return transaction {
            Battles.select { Battles.id eq battleId }.singleOrNull()?.let {
                Battle(
                    id = it[Battles.id],
                    player1Id = it[Battles.player1Id],
                    player2Id = it[Battles.player2Id],
                    status = it[Battles.status],
                    winnerId = it[Battles.winnerId],
                )
            }
        }
    }

    fun endBattle(battleId: String, winnerId: String) {
        transaction {
            Battles.update({ Battles.id eq battleId }) {
                it[status] = "finished"
                it[Battles.winnerId] = winnerId
            }
        }
    }

    fun getLeaderboard(): List<LeaderboardEntry> {
        return transaction {
            val winCount = Battles.winnerId.count()
            Battles
                .slice(Battles.winnerId, winCount)
                .select { Battles.winnerId.isNotNull() and (Battles.status eq "finished") }
                .groupBy(Battles.winnerId)
                .orderBy(winCount, SortOrder.DESC)
                .limit(10)
                .mapIndexedNotNull { index, row ->
                    val winnerId = row[Battles.winnerId] ?: return@mapIndexedNotNull null
                    val username = Users.select { Users.id eq winnerId }
                        .singleOrNull()?.get(Users.username) ?: "Unknown"
                    LeaderboardEntry(rank = index + 1, username = username, score = row[winCount].toInt())
                }
        }
    }
}
