package app.deckbox.backend.routes

import app.deckbox.backend.JWT_AUTH
import app.deckbox.backend.services.DeckService
import app.deckbox.backend.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class DeckRequest(val name: String, val cards: List<String>)

fun Route.deckRoutes(deckService: DeckService) {
    route("/decks") {
        authenticate(JWT_AUTH) {
            post {
                val request = call.receive<DeckRequest>()
                val userId = call.userId()
                val deck = deckService.createDeck(userId, request.name, request.cards)
                call.respond(HttpStatusCode.Created, deck)
            }

            get {
                val userId = call.userId()
                call.respond(deckService.getUserDecks(userId))
            }

            get("/{id}") {
                val deckId = call.parameters["id"]!!
                val userId = call.userId()
                val deck = deckService.getDeck(deckId)
                if (deck != null && deck.userId == userId) {
                    call.respond(deck)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Deck not found"))
                }
            }

            put("/{id}") {
                val deckId = call.parameters["id"]!!
                val userId = call.userId()
                val request = call.receive<DeckRequest>()
                if (deckService.updateDeck(deckId, userId, request.name, request.cards)) {
                    call.respond(mapOf("message" to "Deck updated"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Deck not found or unauthorized"))
                }
            }

            delete("/{id}") {
                val deckId = call.parameters["id"]!!
                val userId = call.userId()
                if (deckService.deleteDeck(deckId, userId)) {
                    call.respond(mapOf("message" to "Deck deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Deck not found or unauthorized"))
                }
            }
        }
    }
}
