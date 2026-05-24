package app.deckbox.backend.services

import app.deckbox.backend.JwtConfig
import app.deckbox.backend.database.*
import app.deckbox.backend.models.*
import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.random.Random

class UserService {
    fun register(username: String, email: String, password: String): User? {
        return transaction {
            // Vérification du nom d'utilisateur
            if (Users.select { Users.username eq username }.count() > 0L) {
                throw IllegalArgumentException("Ce nom d'utilisateur est déjà pris. Essayez-en un autre.")
            }
            // Vérification de l'email
            if (Users.select { Users.email eq email }.count() > 0L) {
                throw IllegalArgumentException("Cet email est déjà utilisé. Connectez-vous ou utilisez un autre email.")
            }

            val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
            val userId = UUID.randomUUID().toString()

            Users.insert {
                it[id] = userId
                it[Users.username] = username
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
            }

            User(userId, username, email)
        }
    }

    fun login(username: String, password: String): LoginResponse? {
        return transaction {
            val userRow = Users.select { Users.username eq username }.singleOrNull()
            if (userRow != null) {
                val hash = userRow[Users.passwordHash]
                if (BCrypt.verifyer().verify(password.toCharArray(), hash).verified) {
                    val user = User(
                        id = userRow[Users.id],
                        username = userRow[Users.username],
                        email = userRow[Users.email],
                        gold = userRow[Users.gold],
                        experience = userRow[Users.experience],
                        premiumPassActive = userRow[Users.premiumPassActive],
                        dailyRewardClaimed = userRow[Users.dailyRewardClaimed],
                    )
                    LoginResponse(token = JwtConfig.generateToken(user.id), user = user)
                } else null
            } else null
        }
    }

    fun getUser(id: String): User? {
        return transaction {
            Users.select { Users.id eq id }.singleOrNull()?.let {
                User(
                    id = it[Users.id],
                    username = it[Users.username],
                    email = it[Users.email],
                    gold = it[Users.gold],
                    experience = it[Users.experience],
                    premiumPassActive = it[Users.premiumPassActive],
                    dailyRewardClaimed = it[Users.dailyRewardClaimed],
                )
            }
        }
    }

    fun claimDailyReward(userId: String): Boolean {
        val today = java.time.LocalDate.now().toString()
        return transaction {
            val user = Users.select { Users.id eq userId }.singleOrNull()
            if (user != null && user[Users.lastDailyClaimDate] != today) {
                Users.update({ Users.id eq userId }) {
                    it[Users.dailyRewardClaimed] = true
                    it[Users.lastDailyClaimDate] = today
                    it[gold] = user[Users.gold] + 10
                }
                true
            } else false
        }
    }

    fun forgotPassword(email: String): String? {
        return transaction {
            val user = Users.select { Users.email eq email }.singleOrNull() ?: return@transaction null
            val code = Random.nextInt(100000, 999999).toString()
            val expiresAt = System.currentTimeMillis() + 15 * 60 * 1000L // 15 min
            PasswordResets.deleteWhere { PasswordResets.email eq email }
            PasswordResets.insert {
                it[PasswordResets.email] = email
                it[PasswordResets.code] = code
                it[PasswordResets.expiresAt] = expiresAt
            }
            code
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String): Boolean {
        return transaction {
            val now = System.currentTimeMillis()
            val reset = PasswordResets.select {
                (PasswordResets.email eq email) and
                    (PasswordResets.code eq code) and
                    (PasswordResets.expiresAt greater now)
            }.singleOrNull() ?: return@transaction false
            val newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
            Users.update({ Users.email eq email }) {
                it[passwordHash] = newHash
            }
            PasswordResets.deleteWhere { PasswordResets.email eq email }
            true
        }
    }

    fun buyPremiumPass(userId: String): Boolean {
        return transaction {
            val user = Users.select { Users.id eq userId }.singleOrNull()
            if (user != null && user[Users.gold] >= 500 && !user[Users.premiumPassActive]) {
                Users.update({ Users.id eq userId }) {
                    it[Users.premiumPassActive] = true
                    it[gold] = user[Users.gold] - 500
                }
                true
            } else false
        }
    }
}
