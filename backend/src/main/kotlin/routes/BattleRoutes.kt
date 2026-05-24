package app.deckbox.backend.routes

import app.deckbox.backend.JWT_AUTH
import app.deckbox.backend.JwtConfig
import app.deckbox.backend.game.BattleGameEngine
import app.deckbox.backend.services.BattleService
import app.deckbox.backend.userId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Route.battleRoutes(battleService: BattleService, battleEngine: BattleGameEngine) {
    route("/battles") {
        get("/leaderboard") {
            val leaderboard = battleService.getLeaderboard()
            call.respond(leaderboard)
        }

        authenticate(JWT_AUTH) {
            post("/matchmake") {
                val playerId = call.userId()
                val battle = battleService.matchmake(playerId)
                if (battle != null) {
                    call.respond(battle)
                } else {
                    call.respond(HttpStatusCode.Accepted, mapOf("message" to "Waiting for opponent", "playerId" to playerId))
                }
            }
        }

        // Auth via query param token since HTTP headers aren't supported for WS upgrade
        webSocket("/battle/{battleId}") {
            val battleId = call.parameters["battleId"]!!
            val token = call.request.queryParameters["token"]
            val playerId = token?.let {
                runCatching { JwtConfig.verifier().verify(it).getClaim("userId").asString() }.getOrNull()
            }

            if (playerId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
                return@webSocket
            }

            val battle = battleService.getBattle(battleId)
            if (battle == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Battle not found"))
                return@webSocket
            }

            battleEngine.join(battleId, playerId, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        battleEngine.handleMessage(battleId, playerId, frame.readText())
                    }
                }
            } finally {
                battleEngine.leave(battleId, playerId)
            }
        }
    }
}
