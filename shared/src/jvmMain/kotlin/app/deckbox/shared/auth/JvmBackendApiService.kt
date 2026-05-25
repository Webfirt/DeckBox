package app.deckbox.shared.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket as JavaWebSocket
import java.util.concurrent.CompletionStage

class JvmBackendApiService : BackendApiService {

  private val httpClient = HttpClient.newHttpClient()
  private val json = Json { ignoreUnknownKeys = true }

  @Volatile private var currentWs: JavaWebSocket? = null

  companion object {
    const val BASE_URL = "http://localhost:8080"
  }

  override suspend fun login(username: String, password: String): Result<LoginData> =
    withContext(Dispatchers.IO) {
      runCatching {
        val body = """{"username":"$username","password":"$password"}"""
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/login"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
          val msg = runCatching {
            json.parseToJsonElement(response.body()).jsonObject["error"]?.jsonPrimitive?.content
          }.getOrNull() ?: "Connexion échouée. Vérifiez vos identifiants."
          error(msg)
        }
        val resp = json.parseToJsonElement(response.body()).jsonObject
        val token = resp["token"]!!.jsonPrimitive.content
        val user = resp["user"]!!.jsonObject
        LoginData(
          token = token,
          userId = user["id"]!!.jsonPrimitive.content,
          username = user["username"]!!.jsonPrimitive.content,
          gold = user["gold"]?.jsonPrimitive?.int ?: 100,
          premiumPassActive = user["premiumPassActive"]?.jsonPrimitive?.boolean ?: false,
        )
      }
    }

  override suspend fun register(username: String, email: String, password: String): Result<UserData> =
    withContext(Dispatchers.IO) {
      runCatching {
        val body = """{"username":"$username","email":"$email","password":"$password"}"""
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/register"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..201) {
          val msg = runCatching {
            json.parseToJsonElement(response.body()).jsonObject["error"]?.jsonPrimitive?.content
          }.getOrNull() ?: "Inscription échouée (${response.statusCode()})"
          error(msg)
        }
        val resp = json.parseToJsonElement(response.body()).jsonObject
        UserData(
          id = resp["id"]?.jsonPrimitive?.content ?: "",
          username = resp["username"]?.jsonPrimitive?.content ?: username,
        )
      }
    }

  override suspend fun claimDailyReward(token: String): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/daily/claim"))
          .header("Authorization", "Bearer $token")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{}"))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) error("Failed to claim daily reward")
        val resp = json.parseToJsonElement(response.body()).jsonObject
        resp["gold"]?.jsonPrimitive?.int ?: 10
      }
    }

  override suspend fun buyPremiumPass(token: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/premium/buy"))
          .header("Authorization", "Bearer $token")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{}"))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
          val msg = json.parseToJsonElement(response.body()).jsonObject["error"]
            ?.jsonPrimitive?.content ?: "Purchase failed"
          error(msg)
        }
      }
    }

  override suspend fun getFriends(token: String): Result<List<FriendData>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/friends"))
          .header("Authorization", "Bearer $token")
          .GET()
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return@runCatching emptyList()
        json.parseToJsonElement(response.body()).jsonArray.map { elem ->
          val obj = elem.jsonObject
          FriendData(
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            username = obj["username"]?.jsonPrimitive?.content ?: "Unknown",
          )
        }
      }
    }

  override suspend fun sendFriendRequest(token: String, toUsername: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val body = """{"toUsername":"$toUsername"}"""
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/friends/request"))
          .header("Authorization", "Bearer $token")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        Unit
      }
    }

  override suspend fun forgotPassword(email: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val body = """{"email":"$email"}"""
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/forgot-password"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
          val msg = runCatching {
            json.parseToJsonElement(response.body()).jsonObject["error"]?.jsonPrimitive?.content
          }.getOrNull() ?: "Aucun compte trouvé pour cet email."
          error(msg)
        }
      }
    }

  override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val body = """{"email":"$email","code":"$code","newPassword":"$newPassword"}"""
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/users/reset-password"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
          val msg = runCatching {
            json.parseToJsonElement(response.body()).jsonObject["error"]?.jsonPrimitive?.content
          }.getOrNull() ?: "Code invalide ou expiré."
          error(msg)
        }
      }
    }

  // ── Online Battle ──────────────────────────────────────────────────────────

  override suspend fun matchmake(token: String): Result<MatchmakeResult> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request = HttpRequest.newBuilder()
          .uri(URI.create("$BASE_URL/battles/matchmake"))
          .header("Authorization", "Bearer $token")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{}"))
          .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val body = runCatching { json.parseToJsonElement(response.body()).jsonObject }.getOrNull()
        when (response.statusCode()) {
          200 -> {
            val battleId = body?.get("id")?.jsonPrimitive?.content
              ?: error("Battle ID manquant dans la réponse")
            MatchmakeResult(battleId = battleId, isWaiting = false)
          }
          202 -> MatchmakeResult(battleId = null, isWaiting = true)
          else -> error("Matchmaking échoué (${response.statusCode()})")
        }
      }
    }

  override fun openBattleSocket(battleId: String, token: String): Flow<String> = callbackFlow {
    val listener = object : JavaWebSocket.Listener {
      override fun onOpen(ws: JavaWebSocket) {
        ws.request(1)
      }
      override fun onText(ws: JavaWebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        trySend(data.toString())
        ws.request(1)
        return null
      }
      override fun onClose(ws: JavaWebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
        close()
        return null
      }
      override fun onError(ws: JavaWebSocket, error: Throwable) {
        close(error)
      }
    }

    val ws = runCatching {
      httpClient.newWebSocketBuilder()
        .buildAsync(URI.create("ws://localhost:8080/battles/battle/$battleId?token=$token"), listener)
        .get()
    }.getOrElse { e ->
      close(e)
      return@callbackFlow
    }
    currentWs = ws

    awaitClose {
      runCatching { ws.abort() }
      currentWs = null
    }
  }.flowOn(Dispatchers.IO)

  override suspend fun sendBattleAction(json: String) = withContext(Dispatchers.IO) {
    runCatching { currentWs?.sendText(json, true) }
    Unit
  }

  override fun closeBattleSocket() {
    runCatching { currentWs?.abort() }
    currentWs = null
  }
}
