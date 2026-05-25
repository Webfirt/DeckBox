package app.deckbox.shared.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WhiteFlag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deckbox.common.screens.MatchmakingScreen
import app.deckbox.common.screens.OnlineBattleScreen
import app.deckbox.common.settings.DeckBoxSettings
import app.deckbox.shared.auth.BackendApiService
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ════════════════════════════════════════════════════════════════════════
//  MODÈLES CLIENT (miroir des modèles backend)
// ════════════════════════════════════════════════════════════════════════

private val POKEMON_EMOJI = mapOf(
  "pikachu" to "⚡", "raichu" to "⚡", "charmander" to "🔥",
  "squirtle" to "💧", "bulbasaur" to "🌿", "mewtwo" to "🔮",
  "gengar" to "👻", "machamp" to "💪", "snorlax" to "😴", "eevee" to "🦊",
)

fun pokemonEmoji(name: String?) = POKEMON_EMOJI[name?.lowercase()] ?: "🃏"

data class ClientPlayerState(
  val playerId: String,
  val activePokemon: String?,
  val activePokemonHp: Int,
  val maxHp: Int,
  val hand: List<String>,
  val deckSize: Int,
  val bench: List<String>,
  val prizes: Int,
)

data class ClientGameState(
  val battleId: String,
  val player1: ClientPlayerState,
  val player2: ClientPlayerState,
  val currentTurn: String,
  val turnNumber: Int,
  val phase: String,
  val winnerId: String?,
)

private fun parsePlayerState(obj: kotlinx.serialization.json.JsonObject): ClientPlayerState {
  val hand = obj["hand"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
  val deck = obj["deck"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
  val bench = obj["bench"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
  return ClientPlayerState(
    playerId = obj["playerId"]!!.jsonPrimitive.content,
    activePokemon = obj["activePokemon"]?.jsonPrimitive?.content,
    activePokemonHp = obj["activePokemonHp"]?.jsonPrimitive?.int ?: 120,
    maxHp = 120,
    hand = hand,
    deckSize = deck.size,
    bench = bench,
    prizes = obj["prizes"]?.jsonPrimitive?.int ?: 6,
  )
}

private fun parseGameState(obj: kotlinx.serialization.json.JsonObject): ClientGameState {
  return ClientGameState(
    battleId = obj["battleId"]!!.jsonPrimitive.content,
    player1 = parsePlayerState(obj["player1"]!!.jsonObject),
    player2 = parsePlayerState(obj["player2"]!!.jsonObject),
    currentTurn = obj["currentTurn"]!!.jsonPrimitive.content,
    turnNumber = obj["turnNumber"]?.jsonPrimitive?.int ?: 1,
    phase = obj["phase"]?.jsonPrimitive?.content ?: "waiting",
    winnerId = obj["winnerId"]?.jsonPrimitive?.content,
  )
}

private fun parseServerMessage(jsonString: String): Triple<String, ClientGameState?, String?> {
  return try {
    val obj = Json.parseToJsonElement(jsonString).jsonObject
    val type = obj["type"]!!.jsonPrimitive.content
    val state = obj["state"]?.jsonObject?.let { parseGameState(it) }
    val message = obj["message"]?.jsonPrimitive?.content
    Triple(type, state, message)
  } catch (e: Exception) {
    Triple("UNKNOWN", null, null)
  }
}

private fun buildBattleAction(type: String, cardId: String? = null, damage: Int? = null): String {
  val cardPart = if (cardId != null) ""","cardId":"$cardId"""" else ""
  val damagePart = if (damage != null) ""","damage":$damage""" else ""
  return """{"type":"$type"$cardPart$damagePart}"""
}

// ════════════════════════════════════════════════════════════════════════
//  MATCHMAKING SCREEN
// ════════════════════════════════════════════════════════════════════════

data class MatchmakingUiState(
  val statusMessage: String,
  val elapsedSeconds: Int,
  val isSearching: Boolean,
  val error: String?,
  val eventSink: (MatchmakingUiEvent) -> Unit,
) : CircuitUiState

sealed interface MatchmakingUiEvent {
  data object Cancel : MatchmakingUiEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Matchmaking(state: MatchmakingUiState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text("🔍 Recherche de partie") },
        navigationIcon = {
          IconButton(onClick = { eventSink(MatchmakingUiEvent.Cancel) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
          }
        },
      )
    },
  ) { padding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp),
      ) {
        Text("⚔️", style = MaterialTheme.typography.displayLarge)

        Text(
          text = "Matchmaking",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )

        Text(
          text = state.statusMessage,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        if (state.isSearching) {
          CircularProgressIndicator(modifier = Modifier.size(48.dp))

          val minutes = state.elapsedSeconds / 60
          val secs = state.elapsedSeconds % 60
          Text(
            text = "Temps écoulé : %d:%02d".format(minutes, secs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        state.error?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }

        OutlinedButton(
          onClick = { eventSink(MatchmakingUiEvent.Cancel) },
          modifier = Modifier.width(200.dp).height(52.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text("Annuler")
        }
      }
    }
  }
}

class MatchmakingPresenter(
  private val screen: MatchmakingScreen,
  private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<MatchmakingUiState> {

  @Composable
  override fun present(): MatchmakingUiState {
    var statusMessage by remember { mutableStateOf("Recherche d'un adversaire...") }
    var isSearching by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
      val token = settings.authToken
      if (token == null) {
        error = "Non connecté. Veuillez vous connecter d'abord."
        isSearching = false
        return@LaunchedEffect
      }

      // Compteur de temps
      launch {
        while (isSearching) {
          delay(1000)
          if (isSearching) elapsedSeconds++
        }
      }

      // Polling matchmaking toutes les 2 secondes
      while (isSearching) {
        backendApi.matchmake(token).fold(
          onSuccess = { result ->
            if (result.battleId != null) {
              isSearching = false
              navigator.goTo(OnlineBattleScreen(result.battleId))
            } else {
              statusMessage = "En attente d'un adversaire..."
            }
          },
          onFailure = { t ->
            error = "Erreur : ${t.message}"
            isSearching = false
          },
        )
        if (isSearching) delay(2000)
      }
    }

    return MatchmakingUiState(
      statusMessage = statusMessage,
      elapsedSeconds = elapsedSeconds,
      isSearching = isSearching,
      error = error,
      eventSink = { event ->
        when (event) {
          MatchmakingUiEvent.Cancel -> {
            isSearching = false
            navigator.pop()
          }
        }
      },
    )
  }
}

// ════════════════════════════════════════════════════════════════════════
//  ONLINE BATTLE SCREEN
// ════════════════════════════════════════════════════════════════════════

data class OnlineBattleUiState(
  val myState: ClientPlayerState?,
  val opponentState: ClientPlayerState?,
  val isMyTurn: Boolean,
  val phase: String,
  val turnNumber: Int,
  val winnerId: String?,
  val myPlayerId: String,
  val statusMessage: String,
  val isConnecting: Boolean,
  val battleEnded: Boolean,
  val selectedCard: String?,
  val eventSink: (OnlineBattleUiEvent) -> Unit,
) : CircuitUiState

sealed interface OnlineBattleUiEvent {
  data object DrawCard : OnlineBattleUiEvent
  data class SelectCard(val cardId: String) : OnlineBattleUiEvent
  data object PlaySelectedCard : OnlineBattleUiEvent
  data object Attack : OnlineBattleUiEvent
  data object EndTurn : OnlineBattleUiEvent
  data object Surrender : OnlineBattleUiEvent
  data object GoBack : OnlineBattleUiEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineBattle(state: OnlineBattleUiState, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (state.battleEnded) "⚔️ Partie terminée"
            else "⚔️ Tour ${state.turnNumber} — ${if (state.isMyTurn) "Votre tour" else "Tour adversaire"}",
          )
        },
        navigationIcon = {
          IconButton(onClick = { eventSink(OnlineBattleUiEvent.GoBack) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
          }
        },
      )
    },
  ) { padding ->

    if (state.isConnecting) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
          CircularProgressIndicator(modifier = Modifier.size(48.dp))
          Text(state.statusMessage, style = MaterialTheme.typography.bodyLarge)
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {

        // ── Zone Adversaire ─────────────────────────────────────────────
        PlayerZone(
          playerState = state.opponentState,
          label = "👤 Adversaire",
          isOpponent = true,
          isCurrentTurn = !state.isMyTurn,
        )

        // ── Barre de statut centrale ─────────────────────────────────────
        ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.elevatedCardColors(
            containerColor = if (state.isMyTurn)
              MaterialTheme.colorScheme.primaryContainer
            else
              MaterialTheme.colorScheme.surfaceVariant,
          ),
        ) {
          Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = state.statusMessage,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
            )
            Text(
              text = "Phase : ${state.phase}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // ── Zone Joueur ──────────────────────────────────────────────────
        PlayerZone(
          playerState = state.myState,
          label = "🎮 Moi",
          isOpponent = false,
          isCurrentTurn = state.isMyTurn,
        )

        // ── Main du joueur ───────────────────────────────────────────────
        if ((state.myState?.hand?.isNotEmpty() == true) && !state.battleEnded) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Main (${state.myState.hand.size} cartes) :",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(state.myState.hand) { cardId ->
                HandCard(
                  cardId = cardId,
                  selected = cardId == state.selectedCard,
                  onClick = { eventSink(OnlineBattleUiEvent.SelectCard(cardId)) },
                )
              }
            }
          }
        }

        // ── Boutons d'action ─────────────────────────────────────────────
        if (!state.battleEnded) {
          ActionButtons(state = state, eventSink = eventSink)
        }
      }
    }

    // ── Dialog fin de partie ────────────────────────────────────────────
    if (state.battleEnded) {
      val iWon = state.winnerId == state.myPlayerId
      AlertDialog(
        onDismissRequest = { eventSink(OnlineBattleUiEvent.GoBack) },
        title = {
          Text(
            text = if (iWon) "🏆 Victoire !" else "😔 Défaite",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = if (iWon) "Félicitations ! Vous avez remporté la partie !"
            else "Meilleure chance la prochaine fois !",
          )
        },
        confirmButton = {
          Button(onClick = { eventSink(OnlineBattleUiEvent.GoBack) }) {
            Text("Retour au Lobby")
          }
        },
      )
    }
  }
}

@Composable
private fun PlayerZone(
  playerState: ClientPlayerState?,
  label: String,
  isOpponent: Boolean,
  isCurrentTurn: Boolean,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(
      containerColor = if (isCurrentTurn)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
      else
        MaterialTheme.colorScheme.surface,
    ),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (playerState != null) {
          Text(
            text = "🎖️ ${playerState.prizes} prix  •  📚 ${playerState.deckSize} cartes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (playerState?.activePokemon != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Pokémon actif
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = pokemonEmoji(playerState.activePokemon),
                style = MaterialTheme.typography.headlineSmall,
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = playerState.activePokemon.replaceFirstChar { it.uppercase() },
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
            )
            val hpFraction = (playerState.activePokemonHp.toFloat() / playerState.maxHp).coerceIn(0f, 1f)
            val hpColor = when {
              hpFraction > 0.5f -> Color(0xFF2E7D32)
              hpFraction > 0.25f -> Color(0xFFF57F17)
              else -> Color(0xFFC62828)
            }
            LinearProgressIndicator(
              progress = { hpFraction },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = hpColor,
            )
            Text(
              text = "${playerState.activePokemonHp} / ${playerState.maxHp} HP",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Bench
        if (playerState.bench.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Banc : ",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            playerState.bench.forEach { card ->
              Text(
                text = pokemonEmoji(card),
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      } else {
        Text(
          text = "Aucun Pokémon actif",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun HandCard(
  cardId: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val bgColor = if (selected)
    MaterialTheme.colorScheme.primaryContainer
  else
    MaterialTheme.colorScheme.surfaceVariant

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    modifier = Modifier
      .size(width = 60.dp, height = 80.dp)
      .clickable { onClick() }
      .then(
        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        else Modifier,
      ),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pokemonEmoji(cardId), style = MaterialTheme.typography.titleLarge)
        Text(
          text = cardId.take(6),
          style = MaterialTheme.typography.labelSmall,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun ActionButtons(
  state: OnlineBattleUiState,
  eventSink: (OnlineBattleUiEvent) -> Unit,
) {
  val isMyTurn = state.isMyTurn
  val phase = state.phase
  val myPokemon = state.myState?.activePokemon

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      // Piocher une carte
      Button(
        onClick = { eventSink(OnlineBattleUiEvent.DrawCard) },
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = isMyTurn && phase == "draw",
      ) {
        Text("🃏 Piocher")
      }

      // Jouer une carte sélectionnée
      FilledTonalButton(
        onClick = { eventSink(OnlineBattleUiEvent.PlaySelectedCard) },
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = isMyTurn && phase == "main" && state.selectedCard != null,
      ) {
        Text("▶ Jouer")
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      // Attaquer
      Button(
        onClick = { eventSink(OnlineBattleUiEvent.Attack) },
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = isMyTurn && phase == "main" && myPokemon != null,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Attaquer (30)")
      }

      // Fin de tour
      FilledTonalButton(
        onClick = { eventSink(OnlineBattleUiEvent.EndTurn) },
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = isMyTurn && (phase == "main" || phase == "draw"),
      ) {
        Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Fin de tour")
      }
    }

    // Abandonner
    OutlinedButton(
      onClick = { eventSink(OnlineBattleUiEvent.Surrender) },
      modifier = Modifier.fillMaxWidth().height(44.dp),
      shape = RoundedCornerShape(12.dp),
    ) {
      Icon(Icons.Filled.WhiteFlag, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(4.dp))
      Text("Abandonner", color = MaterialTheme.colorScheme.error)
    }
  }
}

// ════════════════════════════════════════════════════════════════════════
//  ONLINE BATTLE PRESENTER
// ════════════════════════════════════════════════════════════════════════

class OnlineBattlePresenter(
  private val screen: OnlineBattleScreen,
  private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<OnlineBattleUiState> {

  @Composable
  override fun present(): OnlineBattleUiState {
    val myPlayerId = settings.userId ?: ""
    val token = settings.authToken ?: ""

    var gameState by remember { mutableStateOf<ClientGameState?>(null) }
    var statusMessage by remember { mutableStateOf("Connexion au serveur...") }
    var isConnecting by remember { mutableStateOf(true) }
    var battleEnded by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val messagesFlow = remember(screen.battleId, token) {
      backendApi.openBattleSocket(screen.battleId, token)
    }

    // Écouter les messages WebSocket
    LaunchedEffect(messagesFlow) {
      messagesFlow.collect { jsonString ->
        val (type, state, message) = parseServerMessage(jsonString)
        when (type) {
          "GAME_START" -> {
            gameState = state
            isConnecting = false
            statusMessage = if (state?.currentTurn == myPlayerId)
              "🎯 C'est votre tour ! Piochez une carte."
            else
              "⏳ Tour de l'adversaire..."
          }
          "STATE_UPDATE" -> {
            gameState = state
            statusMessage = if (state?.currentTurn == myPlayerId)
              "🎯 C'est votre tour !"
            else
              "⏳ Tour de l'adversaire..."
          }
          "WAITING" -> {
            statusMessage = message ?: "En attente de l'adversaire..."
          }
          "BATTLE_END" -> {
            gameState = state
            battleEnded = true
            isConnecting = false
            val iWon = state?.winnerId == myPlayerId
            statusMessage = if (iWon) "🏆 Vous avez gagné !" else "😔 Vous avez perdu..."
          }
          "ERROR" -> {
            statusMessage = "⚠️ ${message ?: "Erreur inconnue"}"
          }
        }
      }
    }

    // Fermer le socket en quittant l'écran
    DisposableEffect(Unit) {
      onDispose { backendApi.closeBattleSocket() }
    }

    val myState = gameState?.let { gs ->
      if (gs.player1.playerId == myPlayerId) gs.player1 else gs.player2
    }
    val opponentState = gameState?.let { gs ->
      if (gs.player1.playerId == myPlayerId) gs.player2 else gs.player1
    }
    val isMyTurn = gameState?.currentTurn == myPlayerId

    fun sendAction(type: String, cardId: String? = null, damage: Int? = null) {
      scope.launch {
        backendApi.sendBattleAction(buildBattleAction(type, cardId, damage))
      }
    }

    return OnlineBattleUiState(
      myState = myState,
      opponentState = opponentState,
      isMyTurn = isMyTurn,
      phase = gameState?.phase ?: "waiting",
      turnNumber = gameState?.turnNumber ?: 1,
      winnerId = gameState?.winnerId,
      myPlayerId = myPlayerId,
      statusMessage = statusMessage,
      isConnecting = isConnecting,
      battleEnded = battleEnded,
      selectedCard = selectedCard,
      eventSink = { event ->
        when (event) {
          OnlineBattleUiEvent.DrawCard -> sendAction("DRAW")
          is OnlineBattleUiEvent.SelectCard -> {
            selectedCard = if (selectedCard == event.cardId) null else event.cardId
          }
          OnlineBattleUiEvent.PlaySelectedCard -> {
            val card = selectedCard
            if (card != null) {
              sendAction("PLAY_CARD", cardId = card)
              selectedCard = null
            }
          }
          OnlineBattleUiEvent.Attack -> sendAction("ATTACK", damage = 30)
          OnlineBattleUiEvent.EndTurn -> sendAction("END_TURN")
          OnlineBattleUiEvent.Surrender -> sendAction("SURRENDER")
          OnlineBattleUiEvent.GoBack -> {
            backendApi.closeBattleSocket()
            navigator.pop()
          }
        }
      },
    )
  }
}
