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
import kotlinx.serialization.Serializable

@Serializable
data class BuyRequest(val itemId: String)

fun Route.shopRoutes(shopService: ShopService) {
    route("/shop") {
        get("/items") {
            val items = shopService.getShopItems()
            call.respond(items)
        }

        authenticate(JWT_AUTH) {
            post("/buy") {
                val request = call.receive<BuyRequest>()
                val userId = call.userId()
                if (shopService.buyItem(userId, request.itemId)) {
                    call.respond(mapOf("message" to "Item purchased successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Purchase failed")
                }
            }
        }
    }
}
