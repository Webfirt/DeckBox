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
data class RegisterRequest(val username: String, val email: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

@Serializable
data class TradeRequest(val receiverId: String, val offeredCards: List<String>, val requestedCards: List<String>)

@Serializable
data class FriendRequestBody(val receiverId: String)

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            try {
                val user = userService.register(request.username, request.email, request.password)
                if (user != null) {
                    call.respond(HttpStatusCode.Created, user)
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Inscription échouée. Réessayez."))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "Inscription échouée.")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur serveur. Réessayez plus tard."))
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = userService.login(request.username, request.password)
            if (response != null) {
                call.respond(response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Nom d'utilisateur ou mot de passe incorrect."))
            }
        }

        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            val code = userService.forgotPassword(request.email)
            if (code != null) {
                call.respond(mapOf("code" to code, "message" to "Code de réinitialisation généré"))
            } else {
                call.respond(HttpStatusCode.NotFound, "Aucun compte trouvé pour cet email")
            }
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            if (userService.resetPassword(request.email, request.code, request.newPassword)) {
                call.respond(mapOf("message" to "Mot de passe réinitialisé avec succès"))
            } else {
                call.respond(HttpStatusCode.BadRequest, "Code invalide ou expiré")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            val user = userService.getUser(id)
            if (user != null) {
                call.respond(user)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        authenticate(JWT_AUTH) {
            post("/daily/claim") {
                val userId = call.userId()
                if (userService.claimDailyReward(userId)) {
                    call.respond(mapOf("message" to "Daily reward claimed"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Already claimed or user not found")
                }
            }

            post("/premium/buy") {
                val userId = call.userId()
                if (userService.buyPremiumPass(userId)) {
                    call.respond(mapOf("message" to "Premium pass purchased"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Purchase failed")
                }
            }
        }
    }
}
