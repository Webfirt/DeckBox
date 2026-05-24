package app.deckbox.backend.game

import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(
    val playerId: String,
    val activePokemon: String? = null,
    val activePokemonHp: Int = 120,
    val hand: List<String> = emptyList(),
    val deck: List<String> = emptyList(),
    val bench: List<String> = emptyList(),
    val prizes: Int = 6,
)

@Serializable
data class GameState(
    val battleId: String,
    val player1: PlayerState,
    val player2: PlayerState,
    val currentTurn: String,
    val turnNumber: Int = 1,
    val phase: String = "waiting", // waiting, draw, main, attack, finished
    val winnerId: String? = null,
) {
    companion object {
        private val STARTER_CARDS = listOf(
            "pikachu", "raichu", "charmander", "squirtle", "bulbasaur",
            "mewtwo", "gengar", "machamp", "snorlax", "eevee",
        )

        fun initialize(battleId: String, player1Id: String, player2Id: String): GameState {
            val deck = (STARTER_CARDS + STARTER_CARDS).shuffled()
            return GameState(
                battleId = battleId,
                player1 = PlayerState(
                    playerId = player1Id,
                    activePokemon = STARTER_CARDS.random(),
                    activePokemonHp = 120,
                    hand = deck.take(7),
                    deck = deck.drop(7),
                ),
                player2 = PlayerState(
                    playerId = player2Id,
                    activePokemon = STARTER_CARDS.random(),
                    activePokemonHp = 120,
                    hand = deck.take(7),
                    deck = deck.drop(7),
                ),
                currentTurn = player1Id,
                phase = "draw",
            )
        }
    }
}

// Client → Server
@Serializable
data class BattleMessage(
    val type: String,       // ATTACK, PLAY_CARD, DRAW, END_TURN, SURRENDER
    val cardId: String? = null,
    val damage: Int? = null,
    val attackName: String? = null,
)

// Server → Client
@Serializable
data class ServerMessage(
    val type: String,       // STATE_UPDATE, GAME_START, BATTLE_END, ERROR, WAITING
    val state: GameState? = null,
    val winnerId: String? = null,
    val message: String? = null,
)
