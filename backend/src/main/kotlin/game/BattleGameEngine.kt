package app.deckbox.backend.game

import app.deckbox.backend.database.Battles
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

class BattleGameEngine {
    private val sessions = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>>()
    private val gameStates = ConcurrentHashMap<String, GameState>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(battleId: String) = locks.getOrPut(battleId) { Mutex() }

    suspend fun join(battleId: String, playerId: String, session: WebSocketSession) {
        lockFor(battleId).withLock {
            sessions.getOrPut(battleId) { ConcurrentHashMap() }[playerId] = session

            val battleSessions = sessions[battleId]!!
            val currentState = gameStates[battleId]

            if (currentState == null && battleSessions.size >= 2) {
                val battle = transaction {
                    Battles.select { Battles.id eq battleId }.singleOrNull()
                }
                if (battle != null) {
                    val state = GameState.initialize(
                        battleId = battleId,
                        player1Id = battle[Battles.player1Id],
                        player2Id = battle[Battles.player2Id],
                    )
                    gameStates[battleId] = state
                    broadcast(battleId, ServerMessage(type = "GAME_START", state = state))
                    return@withLock
                }
            }

            val state = gameStates[battleId]
            if (state != null) {
                session.sendMessage(ServerMessage(type = "STATE_UPDATE", state = state))
            } else {
                session.sendMessage(ServerMessage(type = "WAITING", message = "Waiting for opponent to connect"))
            }
        }
    }

    suspend fun handleMessage(battleId: String, playerId: String, text: String) {
        lockFor(battleId).withLock {
            val state = gameStates[battleId] ?: return@withLock
            val msg = runCatching { Json.decodeFromString<BattleMessage>(text) }.getOrNull() ?: return@withLock

            if (msg.type != "SURRENDER" && state.currentTurn != playerId) {
                sessions[battleId]?.get(playerId)?.sendMessage(
                    ServerMessage(type = "ERROR", message = "Not your turn"),
                )
                return@withLock
            }

            val updated = when (msg.type) {
                "DRAW" -> processDraw(state, playerId)
                "PLAY_CARD" -> processPlayCard(state, playerId, msg)
                "ATTACK" -> processAttack(state, playerId, msg)
                "END_TURN" -> processEndTurn(state, playerId)
                "SURRENDER" -> processSurrender(state, playerId)
                else -> null
            } ?: return@withLock

            gameStates[battleId] = updated

            if (updated.phase == "finished") {
                broadcast(battleId, ServerMessage(type = "BATTLE_END", state = updated, winnerId = updated.winnerId))
            } else {
                broadcast(battleId, ServerMessage(type = "STATE_UPDATE", state = updated))
            }
        }
    }

    fun leave(battleId: String, playerId: String) {
        sessions[battleId]?.remove(playerId)
        if (sessions[battleId]?.isEmpty() == true) {
            sessions.remove(battleId)
            gameStates.remove(battleId)
            locks.remove(battleId)
        }
    }

    private fun processDraw(state: GameState, playerId: String): GameState {
        val player = state.playerFor(playerId)
        if (player.deck.isEmpty()) {
            val winnerId = state.opponentOf(playerId)
            return state.copy(phase = "finished", winnerId = winnerId)
        }
        val drawn = player.deck.first()
        val updated = player.copy(deck = player.deck.drop(1), hand = player.hand + drawn)
        return state.withUpdatedPlayer(playerId, updated).copy(phase = "main")
    }

    private fun processPlayCard(state: GameState, playerId: String, msg: BattleMessage): GameState {
        val cardId = msg.cardId ?: return state
        val player = state.playerFor(playerId)
        if (cardId !in player.hand) return state

        val updatedHand = player.hand - cardId
        val updated = when {
            player.activePokemon == null -> player.copy(hand = updatedHand, activePokemon = cardId, activePokemonHp = 120)
            player.bench.size < 5 -> player.copy(hand = updatedHand, bench = player.bench + cardId)
            else -> return state
        }
        return state.withUpdatedPlayer(playerId, updated)
    }

    private fun processAttack(state: GameState, playerId: String, msg: BattleMessage): GameState {
        val damage = msg.damage ?: 30
        val opponentId = state.opponentOf(playerId)
        val opponent = state.playerFor(opponentId)
        val newHp = maxOf(0, opponent.activePokemonHp - damage)
        val updatedOpponent = opponent.copy(activePokemonHp = newHp)

        return if (newHp <= 0) {
            state.withUpdatedPlayer(opponentId, updatedOpponent)
                .copy(phase = "finished", winnerId = playerId)
        } else {
            val nextTurn = opponentId
            state.withUpdatedPlayer(opponentId, updatedOpponent)
                .copy(phase = "draw", currentTurn = nextTurn, turnNumber = state.turnNumber + 1)
        }
    }

    private fun processEndTurn(state: GameState, playerId: String): GameState {
        val nextTurn = state.opponentOf(playerId)
        return state.copy(phase = "draw", currentTurn = nextTurn, turnNumber = state.turnNumber + 1)
    }

    private fun processSurrender(state: GameState, playerId: String): GameState {
        return state.copy(phase = "finished", winnerId = state.opponentOf(playerId))
    }

    private suspend fun broadcast(battleId: String, msg: ServerMessage) {
        val json = Json.encodeToString(msg)
        sessions[battleId]?.values?.forEach { session ->
            runCatching { session.send(Frame.Text(json)) }
        }
    }

    private suspend fun WebSocketSession.sendMessage(msg: ServerMessage) {
        runCatching { send(Frame.Text(Json.encodeToString(msg))) }
    }
}

private fun GameState.playerFor(playerId: String) =
    if (player1.playerId == playerId) player1 else player2

private fun GameState.opponentOf(playerId: String) =
    if (player1.playerId == playerId) player2.playerId else player1.playerId

private fun GameState.withUpdatedPlayer(playerId: String, updated: PlayerState) =
    if (player1.playerId == playerId) copy(player1 = updated) else copy(player2 = updated)
