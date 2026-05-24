package app.deckbox.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private val SECRET: String = System.getenv("DECKBOX_JWT_SECRET") ?: "deckbox-secret-change-in-production"
    const val AUDIENCE = "deckbox-users"
    const val ISSUER = "deckbox-backend"
    private val algorithm = Algorithm.HMAC256(SECRET)

    fun generateToken(userId: String): String = JWT.create()
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
        .sign(algorithm)

    fun verifier() = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()
}
