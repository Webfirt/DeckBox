package app.deckbox.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        val allowedOrigin = System.getenv("DECKBOX_ALLOWED_ORIGIN")
        if (allowedOrigin != null) {
            val host = allowedOrigin.removePrefix("https://").removePrefix("http://")
            val scheme = if (allowedOrigin.startsWith("https")) "https" else "http"
            allowHost(host, schemes = listOf(scheme))
        } else {
            anyHost() // Development: allow all origins
        }
    }
}
