package app.deckbox.shared.auth

interface BackendApiService {
  suspend fun login(username: String, password: String): Result<LoginData>
  suspend fun register(username: String, email: String, password: String): Result<UserData>
  suspend fun claimDailyReward(token: String): Result<Int>
  suspend fun buyPremiumPass(token: String): Result<Unit>
  suspend fun getFriends(token: String): Result<List<FriendData>>
  suspend fun sendFriendRequest(token: String, toUsername: String): Result<Unit>
  suspend fun forgotPassword(email: String): Result<Unit>
  suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>
}

data class LoginData(
  val token: String,
  val userId: String,
  val username: String,
  val gold: Int,
  val premiumPassActive: Boolean,
)

data class UserData(
  val id: String,
  val username: String,
)

data class FriendData(
  val id: String,
  val username: String,
)
