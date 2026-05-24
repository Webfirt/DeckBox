package app.deckbox.backend.routes

import app.deckbox.backend.JWT_AUTH
import app.deckbox.backend.services.*
import app.deckbox.backend.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tradeRoutes(tradeService: TradeService) {
    route("/trades") {
        authenticate(JWT_AUTH) {
            post("/send") {
                val request = call.receive<TradeRequest>()
                val senderId = call.userId()
                val trade = tradeService.sendTrade(
                    senderId = senderId,
                    receiverId = request.receiverId,
                    offeredCards = request.offeredCards,
                    requestedCards = request.requestedCards,
                )
                call.respond(HttpStatusCode.Created, trade)
            }

            post("/accept/{tradeId}") {
                val tradeId = call.parameters["tradeId"]!!
                if (tradeService.acceptTrade(tradeId)) {
                    call.respond(mapOf("message" to "Trade accepted"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Trade acceptance failed")
                }
            }

            get("/pending") {
                val userId = call.userId()
                val trades = tradeService.getPendingTrades(userId)
                call.respond(trades)
            }
        }
    }
}
