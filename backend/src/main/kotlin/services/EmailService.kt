package app.deckbox.backend.services

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

object EmailService {

    private val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
    private val smtpPort = System.getenv("SMTP_PORT") ?: "587"
    private val smtpUser = System.getenv("SMTP_USER") ?: ""
    private val smtpPass = System.getenv("SMTP_PASS") ?: ""

    /**
     * Envoie un email avec le code de réinitialisation.
     * Si SMTP n'est pas configuré (dev), affiche le code dans les logs.
     */
    fun sendPasswordResetCode(toEmail: String, code: String) {
        if (smtpUser.isBlank() || smtpPass.isBlank()) {
            println("========================================")
            println("[EmailService] MODE DEV — SMTP non configuré")
            println("[EmailService] Email destinataire : $toEmail")
            println("[EmailService] Code de réinitialisation : $code")
            println("========================================")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.ssl.trust", smtpHost)
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(smtpUser, smtpPass)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(smtpUser, "DeckBox Pokémon TCG"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            subject = "🔑 Code de réinitialisation - DeckBox"
            setContent(buildHtmlEmail(code), "text/html; charset=utf-8")
        }

        Transport.send(message)
        println("[EmailService] Email envoyé à $toEmail")
    }

    private fun buildHtmlEmail(code: String) = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"></head>
        <body style="margin:0;padding:0;background:#f0f0f0;font-family:Arial,sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0">
            <tr><td align="center" style="padding:40px 20px;">
              <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.10);">
                <tr><td style="padding:40px 40px 0 40px;text-align:center;">
                  <div style="font-size:56px;">🃏</div>
                  <h1 style="color:#1a1a2e;font-size:24px;margin:12px 0 4px;">DeckBox Pokémon TCG</h1>
                  <p style="color:#888;font-size:14px;margin:0;">Réinitialisation de mot de passe</p>
                </td></tr>
                <tr><td style="padding:32px 40px;">
                  <hr style="border:none;border-top:1px solid #eee;margin:0 0 24px;">
                  <p style="color:#444;font-size:15px;margin:0 0 8px;">Bonjour,</p>
                  <p style="color:#444;font-size:15px;margin:0 0 24px;">
                    Voici votre code de vérification pour réinitialiser votre mot de passe.
                    Il est valable pendant <strong>15 minutes</strong>.
                  </p>
                  <div style="text-align:center;margin:32px 0;">
                    <div style="display:inline-block;background:#f0e6ff;border-radius:14px;padding:18px 40px;">
                      <span style="font-size:40px;font-weight:900;letter-spacing:10px;color:#6200ea;">$code</span>
                    </div>
                  </div>
                  <p style="color:#888;font-size:13px;margin:24px 0 0;">
                    ⚠️ Si vous n'avez pas demandé cette réinitialisation, ignorez cet email. Votre mot de passe reste inchangé.
                  </p>
                </td></tr>
                <tr><td style="padding:16px 40px 32px;text-align:center;border-top:1px solid #f0f0f0;">
                  <p style="color:#bbb;font-size:12px;margin:0;">© 2025 DeckBox Pokémon TCG. Tous droits réservés.</p>
                </td></tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
