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

fun Route.friendRoutes(friendService: FriendService) {
    route("/friends") {
        authenticate(JWT_AUTH) {
            post("/add") {
                val request = call.receive<FriendRequestBody>()
                val senderId = call.userId()
                val friendRequest = friendService.sendFriendRequest(senderId, request.receiverId)
                call.respond(HttpStatusCode.Created, friendRequest)
            }

            post("/accept/{requestId}") {
                val requestId = call.parameters["requestId"]!!
                if (friendService.acceptFriendRequest(requestId)) {
                    call.respond(mapOf("message" to "Friend request accepted"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Acceptance failed")
                }
            }

            get("/pending") {
                val userId = call.userId()
                val requests = friendService.getPendingRequests(userId)
                call.respond(requests)
            }

            get("/list") {
                val userId = call.userId()
                val friends = friendService.getFriends(userId)
                call.respond(friends)
            }
        }
    }
}
