package app.deckbox.shared.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import app.deckbox.common.screens.DecksScreen
import app.deckbox.common.screens.ForgotPasswordScreen
import app.deckbox.common.screens.LoginScreen
import app.deckbox.common.screens.RegisterScreen
import app.deckbox.common.settings.DeckBoxSettings
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

// ─────────────── LOGIN ───────────────

data class LoginUiState(
  val username: String,
  val password: String,
  val isLoading: Boolean,
  val error: String?,
  val eventSink: (LoginUiEvent) -> Unit,
) : CircuitUiState

sealed interface LoginUiEvent {
  data class SetUsername(val value: String) : LoginUiEvent
  data class SetPassword(val value: String) : LoginUiEvent
  data object Submit : LoginUiEvent
  data object GoRegister : LoginUiEvent
  data object GoForgotPassword : LoginUiEvent
  data object PlayOffline : LoginUiEvent
}

@Composable
fun Login(state: LoginUiState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  val strings = LocalStrings.current
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(32.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(text = "🃏", style = MaterialTheme.typography.displayMedium)
        Text(
          text = strings.loginTitle,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = strings.loginSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        var showPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
          value = state.username,
          onValueChange = { eventSink(LoginUiEvent.SetUsername(it)) },
          label = { Text(strings.loginUsernameLabel) },
          leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = state.password,
          onValueChange = { eventSink(LoginUiEvent.SetPassword(it)) },
          label = { Text(strings.loginPasswordLabel) },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(
                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
              )
            }
          },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }

        Button(
          onClick = { eventSink(LoginUiEvent.Submit) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(16.dp),
          enabled = !state.isLoading && state.username.isNotBlank() && state.password.isNotBlank(),
        ) {
          if (state.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(strings.loginButton, style = MaterialTheme.typography.titleMedium)
          }
        }

        TextButton(onClick = { eventSink(LoginUiEvent.GoForgotPassword) }) {
          Text(strings.loginForgotPassword, color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = { eventSink(LoginUiEvent.GoRegister) }) {
          Text(strings.loginGoRegister)
        }
        TextButton(onClick = { eventSink(LoginUiEvent.PlayOffline) }) {
          Text(strings.loginPlayOffline, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

class LoginPresenter(
  private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<LoginUiState> {
  @Composable
  override fun present(): LoginUiState {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    return LoginUiState(
      username = username,
      password = password,
      isLoading = isLoading,
      error = error,
      eventSink = { event ->
        when (event) {
          is LoginUiEvent.SetUsername -> { username = event.value; error = null }
          is LoginUiEvent.SetPassword -> { password = event.value; error = null }
          LoginUiEvent.GoRegister -> navigator.goTo(RegisterScreen())
          LoginUiEvent.GoForgotPassword -> navigator.goTo(ForgotPasswordScreen())
          LoginUiEvent.PlayOffline -> navigator.goTo(DecksScreen())
          LoginUiEvent.Submit -> {
            if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
              scope.launch {
                isLoading = true
                error = null
                backendApi.login(username.trim(), password).fold(
                  onSuccess = { data ->
                    settings.authToken = data.token
                    settings.userId = data.userId
                    settings.playerName = data.username
                    settings.goldBalance = data.gold
                    settings.premiumPassActive = data.premiumPassActive
                    navigator.goTo(DecksScreen())
                  },
                  onFailure = { t ->
                    error = t.message ?: "Connexion échouée. Vérifiez vos identifiants."
                    isLoading = false
                  },
                )
              }
            }
          }
        }
      },
    )
  }
}

// ─────────────── REGISTER ───────────────

data class RegisterUiState(
  val username: String,
  val email: String,
  val password: String,
  val isLoading: Boolean,
  val error: String?,
  val eventSink: (RegisterUiEvent) -> Unit,
) : CircuitUiState

sealed interface RegisterUiEvent {
  data class SetUsername(val value: String) : RegisterUiEvent
  data class SetEmail(val value: String) : RegisterUiEvent
  data class SetPassword(val value: String) : RegisterUiEvent
  data object Submit : RegisterUiEvent
  data object GoLogin : RegisterUiEvent
}

@Composable
fun Register(state: RegisterUiState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  val strings = LocalStrings.current
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(32.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(text = "✨", style = MaterialTheme.typography.displayMedium)
        Text(
          text = strings.registerTitle,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = strings.registerSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        var showPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
          value = state.username,
          onValueChange = { eventSink(RegisterUiEvent.SetUsername(it)) },
          label = { Text(strings.loginUsernameLabel) },
          leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = state.email,
          onValueChange = { eventSink(RegisterUiEvent.SetEmail(it)) },
          label = { Text(strings.registerEmailLabel) },
          leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = state.password,
          onValueChange = { eventSink(RegisterUiEvent.SetPassword(it)) },
          label = { Text(strings.loginPasswordLabel) },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
              Icon(
                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
              )
            }
          },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }

        Button(
          onClick = { eventSink(RegisterUiEvent.Submit) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(16.dp),
          enabled = !state.isLoading &&
            state.username.isNotBlank() &&
            state.email.isNotBlank() &&
            state.password.isNotBlank(),
        ) {
          if (state.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(strings.registerCreateButton, style = MaterialTheme.typography.titleMedium)
          }
        }

        TextButton(onClick = { eventSink(RegisterUiEvent.GoLogin) }) {
          Text(strings.registerGoLogin)
        }
      }
    }
  }
}

class RegisterPresenter(
  private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<RegisterUiState> {
  @Composable
  override fun present(): RegisterUiState {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    return RegisterUiState(
      username = username,
      email = email,
      password = password,
      isLoading = isLoading,
      error = error,
      eventSink = { event ->
        when (event) {
          is RegisterUiEvent.SetUsername -> { username = event.value; error = null }
          is RegisterUiEvent.SetEmail -> { email = event.value; error = null }
          is RegisterUiEvent.SetPassword -> { password = event.value; error = null }
          RegisterUiEvent.GoLogin -> navigator.pop()
          RegisterUiEvent.Submit -> {
            if (!isLoading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
              scope.launch {
                isLoading = true
                error = null
                backendApi.register(username.trim(), email.trim(), password).fold(
                  onSuccess = {
                    backendApi.login(username.trim(), password).fold(
                      onSuccess = { data ->
                        settings.authToken = data.token
                        settings.userId = data.userId
                        settings.playerName = data.username
                        settings.goldBalance = data.gold
                        settings.premiumPassActive = data.premiumPassActive
                        navigator.goTo(DecksScreen())
                      },
                      onFailure = { navigator.pop() },
                    )
                  },
                  onFailure = { t ->
                    error = t.message ?: "Inscription échouée. Essayez un autre nom d'utilisateur."
                    isLoading = false
                  },
                )
              }
            }
          }
        }
      },
    )
  }
}

// ─────────────── FORGOT PASSWORD ───────────────

data class ForgotPasswordUiState(
  val email: String,
  val code: String,
  val newPassword: String,
  val isLoading: Boolean,
  val error: String?,
  val resetCodeSent: Boolean,
  val success: Boolean,
  val eventSink: (ForgotPasswordUiEvent) -> Unit,
) : CircuitUiState

sealed interface ForgotPasswordUiEvent {
  data class SetEmail(val value: String) : ForgotPasswordUiEvent
  data class SetCode(val value: String) : ForgotPasswordUiEvent
  data class SetNewPassword(val value: String) : ForgotPasswordUiEvent
  data object SendCode : ForgotPasswordUiEvent
  data object ResetPassword : ForgotPasswordUiEvent
  data object GoLogin : ForgotPasswordUiEvent
}

@Composable
fun ForgotPassword(state: ForgotPasswordUiState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  val strings = LocalStrings.current
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
      Column(
        modifier = Modifier
          .padding(32.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(text = "🔑", style = MaterialTheme.typography.displayMedium)
        Text(
          text = strings.forgotPasswordTitle,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )

        if (state.success) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = "✅ Mot de passe réinitialisé avec succès !",
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
          )
          Spacer(Modifier.height(8.dp))
          Button(
            onClick = { eventSink(ForgotPasswordUiEvent.GoLogin) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(strings.forgotPasswordGoLogin)
          }
        } else if (!state.resetCodeSent) {
          // ── Étape 1 : saisie de l'email ──
          Text(
            text = strings.forgotPasswordSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(4.dp))
          OutlinedTextField(
            value = state.email,
            onValueChange = { eventSink(ForgotPasswordUiEvent.SetEmail(it)) },
            label = { Text(strings.forgotPasswordEmailLabel) },
            leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          state.error?.let {
            Text(
              text = it,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
            )
          }
          Button(
            onClick = { eventSink(ForgotPasswordUiEvent.SendCode) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !state.isLoading && state.email.isNotBlank(),
          ) {
            if (state.isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text(strings.forgotPasswordSendButton, style = MaterialTheme.typography.titleMedium)
            }
          }
          TextButton(onClick = { eventSink(ForgotPasswordUiEvent.GoLogin) }) {
            Text(strings.forgotPasswordGoLogin)
          }
        } else {
          // ── Étape 2 : saisie du code + nouveau mot de passe ──
          Text(
            text = "📧 Un code a été envoyé à",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Text(
            text = state.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Vérifiez votre boîte mail (et les spams).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(4.dp))

          var showPassword by remember { mutableStateOf(false) }

          // Champ code
          OutlinedTextField(
            value = state.code,
            onValueChange = { eventSink(ForgotPasswordUiEvent.SetCode(it)) },
            label = { Text(strings.forgotPasswordCodeHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )

          // Champ nouveau mot de passe
          val passwordOk = state.newPassword.length >= 8 &&
            state.newPassword.any { it.isLetter() } &&
            state.newPassword.any { it.isDigit() }
          val passwordColor = when {
            state.newPassword.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
            passwordOk -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
            else -> MaterialTheme.colorScheme.error
          }

          OutlinedTextField(
            value = state.newPassword,
            onValueChange = { eventSink(ForgotPasswordUiEvent.SetNewPassword(it)) },
            label = { Text(strings.forgotPasswordNewPasswordLabel) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                  contentDescription = null,
                )
              }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
              Text(
                text = if (passwordOk) "✅ Mot de passe valide"
                       else "Min. 8 caractères avec au moins une lettre et un chiffre",
                color = passwordColor,
                style = MaterialTheme.typography.bodySmall,
              )
            },
          )

          state.error?.let {
            Text(
              text = it,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
            )
          }

          Button(
            onClick = { eventSink(ForgotPasswordUiEvent.ResetPassword) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !state.isLoading && state.code.isNotBlank() && passwordOk,
          ) {
            if (state.isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text(strings.forgotPasswordResetButton, style = MaterialTheme.typography.titleMedium)
            }
          }

          TextButton(onClick = { eventSink(ForgotPasswordUiEvent.SendCode) }) {
            Text("Renvoyer le code", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

class ForgotPasswordPresenter(
  private val navigator: Navigator,
  private val backendApi: BackendApiService,
) : Presenter<ForgotPasswordUiState> {
  @Composable
  override fun present(): ForgotPasswordUiState {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resetCodeSent by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    return ForgotPasswordUiState(
      email = email,
      code = code,
      newPassword = newPassword,
      isLoading = isLoading,
      error = error,
      resetCodeSent = resetCodeSent,
      success = success,
      eventSink = { event ->
        when (event) {
          is ForgotPasswordUiEvent.SetEmail -> { email = event.value; error = null }
          is ForgotPasswordUiEvent.SetCode -> { code = event.value; error = null }
          is ForgotPasswordUiEvent.SetNewPassword -> { newPassword = event.value; error = null }
          ForgotPasswordUiEvent.GoLogin -> navigator.goTo(LoginScreen())
          ForgotPasswordUiEvent.SendCode -> {
            if (!isLoading && email.isNotBlank()) {
              scope.launch {
                isLoading = true
                error = null
                backendApi.forgotPassword(email.trim()).fold(
                  onSuccess = {
                    resetCodeSent = true
                    isLoading = false
                  },
                  onFailure = { t ->
                    error = t.message ?: "Aucun compte trouvé pour cet email."
                    isLoading = false
                  },
                )
              }
            }
          }
          ForgotPasswordUiEvent.ResetPassword -> {
            if (!isLoading && code.isNotBlank() && newPassword.isNotBlank()) {
              scope.launch {
                isLoading = true
                error = null
                backendApi.resetPassword(email.trim(), code.trim(), newPassword).fold(
                  onSuccess = {
                    success = true
                    isLoading = false
                  },
                  onFailure = { t ->
                    error = t.message ?: "Code invalide ou expiré."
                    isLoading = false
                  },
                )
              }
            }
          }
        }
      },
    )
  }
}
