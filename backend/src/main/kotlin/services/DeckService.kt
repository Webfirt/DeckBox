package app.deckbox.backend.services

import app.deckbox.backend.database.Decks
import app.deckbox.backend.models.Deck
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DeckService {
    fun createDeck(userId: String, name: String, cards: List<String>): Deck {
        val deckId = UUID.randomUUID().toString()
        return transaction {
            Decks.insert {
                it[id] = deckId
                it[Decks.userId] = userId
                it[Decks.name] = name
                it[Decks.cards] = cards.joinToString(",")
            }
            Deck(deckId, userId, name, cards)
        }
    }

    fun getUserDecks(userId: String): List<Deck> = transaction {
        Decks.select { Decks.userId eq userId }.map { it.toDeck() }
    }

    fun getDeck(deckId: String): Deck? = transaction {
        Decks.select { Decks.id eq deckId }.singleOrNull()?.toDeck()
    }

    fun updateDeck(deckId: String, userId: String, name: String, cards: List<String>): Boolean = transaction {
        Decks.update({ (Decks.id eq deckId) and (Decks.userId eq userId) }) {
            it[Decks.name] = name
            it[Decks.cards] = cards.joinToString(",")
        } > 0
    }

    fun deleteDeck(deckId: String, userId: String): Boolean = transaction {
        Decks.deleteWhere { (Decks.id eq deckId) and (Decks.userId eq userId) } > 0
    }

    private fun ResultRow.toDeck() = Deck(
        id = this[Decks.id],
        userId = this[Decks.userId],
        name = this[Decks.name],
        cards = this[Decks.cards].split(",").filter { it.isNotBlank() },
    )
}
