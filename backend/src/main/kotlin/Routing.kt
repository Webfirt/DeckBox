package app.deckbox.backend

import app.deckbox.backend.game.BattleGameEngine
import app.deckbox.backend.routes.*
import app.deckbox.backend.services.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val userService = UserService()
    val battleService = BattleService()
    val battleEngine = BattleGameEngine()
    val tradeService = TradeService()
    val friendService = FriendService()
    val shopService = ShopService()
    val deckService = DeckService()

    routing {
        get("/") {
            call.respondText("DeckBox Backend API v1.0")
        }

        userRoutes(userService)
        battleRoutes(battleService, battleEngine)
        tradeRoutes(tradeService)
        friendRoutes(friendService)
        shopRoutes(shopService)
        deckRoutes(deckService)
    }
}
