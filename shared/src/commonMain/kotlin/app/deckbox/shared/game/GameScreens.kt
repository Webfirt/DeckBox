package app.deckbox.shared.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deckbox.common.compose.DeckBoxRootAppBar
import app.deckbox.common.compose.overlays.showBottomSheetScreen
import app.deckbox.common.compose.widgets.CollectionBar
import app.deckbox.common.screens.BattleScreen
import app.deckbox.common.screens.CollectionScreen
import app.deckbox.common.screens.DeckPickerScreen
import app.deckbox.common.screens.DecksScreen
import app.deckbox.common.screens.FriendsScreen
import app.deckbox.common.screens.LoginScreen
import app.deckbox.common.screens.MatchmakingScreen
import app.deckbox.common.screens.PlayTestScreen
import app.deckbox.common.screens.ProfileScreen
import app.deckbox.common.screens.SettingsScreen
import app.deckbox.common.screens.ShopScreen
import app.deckbox.common.screens.TournamentsScreen
import app.deckbox.shared.auth.BackendApiService
import app.deckbox.common.settings.DeckBoxSettings
import app.deckbox.core.di.MergeActivityScope
import app.deckbox.expansions.ExpansionsRepository
import app.deckbox.features.collection.api.CollectionRepository
import cafe.adriel.lyricist.LocalStrings
import com.moriatsushi.insetsx.systemBars
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class BattleUiState(
  val leaderboard: List<LeaderboardEntry>,
  val eventSink: (BattleUiEvent) -> Unit,
) : CircuitUiState

data class LeaderboardEntry(
  val rank: Int,
  val playerName: String,
  val wins: Int,
)

sealed interface BattleUiEvent {
  data class StartMatch(val deckId: String) : BattleUiEvent
  data object ChallengeFriend : BattleUiEvent
  data object CreateDeck : BattleUiEvent
  data object OpenSettings : BattleUiEvent
  data object ViewTournaments : BattleUiEvent
}

data class ExpansionProgress(
  val id: String,
  val name: String,
  val symbolUrl: String,
  val ownedCards: Int,
  val totalCards: Int,
) {
  val progressFraction: Float get() = if (totalCards > 0) ownedCards.toFloat() / totalCards else 0f
}

data class CollectionUiState(
  val totalCards: Int,
  val uniqueExpansions: Int,
  val expansions: List<ExpansionProgress>,
  val isLoading: Boolean,
  val dailyRewardClaimed: Boolean,
  val rewardMessage: String?,
  val eventSink: (CollectionUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionUiEvent {
  data object ClaimDailyReward : CollectionUiEvent
  data object OpenSettings : CollectionUiEvent
}

data class ShopUiState(
  val goldBalance: Int,
  val premiumPassPurchased: Boolean,
  val purchaseMessage: String?,
  val shopItems: List<ShopItem>,
  val eventSink: (ShopUiEvent) -> Unit,
) : CircuitUiState

data class ShopItem(
  val name: String,
  val cost: Int,
  val description: String,
)

sealed interface ShopUiEvent {
  data class BuyItem(val item: ShopItem) : ShopUiEvent
  data object BuyPremiumPass : ShopUiEvent
  data object OpenSettings : ShopUiEvent
}

data class ProfileUiState(
  val playerName: String,
  val rank: Int,
  val totalWins: Int,
  val totalCards: Int,
  val goldBalance: Int,
  val premiumPassActive: Boolean,
  val isLoggedIn: Boolean,
  val appLanguage: DeckBoxSettings.AppLanguage,
  val showEditDialog: Boolean = false,
  val selectedAvatar: String = "🎮",
  val eventSink: (ProfileUiEvent) -> Unit,
) : CircuitUiState

sealed interface ProfileUiEvent {
  data object ViewFriends : ProfileUiEvent
  data object OpenSettings : ProfileUiEvent
  data object EditProfile : ProfileUiEvent
  data class SaveProfile(val name: String, val avatar: String) : ProfileUiEvent
  data object DismissEdit : ProfileUiEvent
  data object Logout : ProfileUiEvent
  data class SetLanguage(val language: DeckBoxSettings.AppLanguage) : ProfileUiEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(MergeActivityScope::class, BattleScreen::class)
@Composable
internal fun Battle(
  state: BattleUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  val overlayHost = LocalOverlayHost.current
  val coroutineScope = rememberCoroutineScope()
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = modifier,
    topBar = {
      DeckBoxRootAppBar(
        title = LocalStrings.current.battle,
        actions = {
          IconButton(onClick = { eventSink(BattleUiEvent.OpenSettings) }) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = WindowInsets.systemBars,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxWidth()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero banner
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "⚔️ Battle Lobby",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "Choose your deck and fight!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          )
        }
      }

      // Primary action
      Button(
        onClick = {
          coroutineScope.launch {
            val response = overlayHost.showBottomSheetScreen(DeckPickerScreen())
            when (response) {
              is DeckPickerScreen.Response.Deck -> eventSink(BattleUiEvent.StartMatch(response.deck.id))
              DeckPickerScreen.Response.NewDeck -> eventSink(BattleUiEvent.CreateDeck)
              else -> Unit
            }
          }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "Quick Battle", style = MaterialTheme.typography.titleMedium)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
          onClick = { eventSink(BattleUiEvent.ChallengeFriend) },
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Challenge Friend")
        }
        OutlinedButton(
          onClick = { eventSink(BattleUiEvent.ViewTournaments) },
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(LocalStrings.current.tournamentsTitle)
        }
      }

      HorizontalDivider()

      Text(
        text = "🏆 Leaderboard",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )

      state.leaderboard.forEach { entry ->
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
          ListItem(
            headlineContent = { Text(text = entry.playerName, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(text = "${entry.wins} wins") },
            leadingContent = {
              Surface(
                shape = CircleShape,
                color = when (entry.rank) {
                  1 -> Color(0xFFFFD700)
                  2 -> Color(0xFFC0C0C0)
                  else -> Color(0xFFCD7F32)
                },
                modifier = Modifier.size(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                }
              }
            },
          )
        }
      }
    }
    } // Box
  }
}

@CircuitInject(MergeActivityScope::class, BattleScreen::class)
@Inject
class BattlePresenter(
  @Assisted private val navigator: Navigator,
) : Presenter<BattleUiState> {
  @Composable
  override fun present(): BattleUiState {
    val leaderboard = remember {
      listOf(
        LeaderboardEntry(1, "PikachuChampion", 152),
        LeaderboardEntry(2, "CharizardQueen", 146),
        LeaderboardEntry(3, "MewTwoMaster", 129),
      )
    }

    return BattleUiState(
      leaderboard = leaderboard,
      eventSink = { event ->
        when (event) {
          is BattleUiEvent.StartMatch -> navigator.goTo(MatchmakingScreen(event.deckId))
          BattleUiEvent.ChallengeFriend -> navigator.goTo(FriendsScreen())
          BattleUiEvent.CreateDeck -> navigator.goTo(DecksScreen())
          BattleUiEvent.OpenSettings -> navigator.goTo(SettingsScreen())
          BattleUiEvent.ViewTournaments -> navigator.goTo(TournamentsScreen())
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(MergeActivityScope::class, CollectionScreen::class)
@Composable
internal fun Collection(
  state: CollectionUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = modifier,
    topBar = {
      DeckBoxRootAppBar(
        title = LocalStrings.current.collection,
        actions = {
          IconButton(onClick = { eventSink(CollectionUiEvent.OpenSettings) }) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = WindowInsets.systemBars,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxWidth()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CollectionStatCard(
            icon = Icons.Filled.Casino,
            label = "Total cards",
            value = "${state.totalCards}",
            modifier = Modifier.weight(1f),
          )
          CollectionStatCard(
            icon = Icons.Filled.Collections,
            label = "Expansions",
            value = "${state.uniqueExpansions}",
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = { eventSink(CollectionUiEvent.ClaimDailyReward) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !state.dailyRewardClaimed,
          ) {
            Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (state.dailyRewardClaimed) "✓ Daily Reward Claimed" else "Claim Daily Reward",
              style = MaterialTheme.typography.titleMedium,
            )
          }
          state.rewardMessage?.let {
            Text(
              text = it,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      if (state.expansions.isNotEmpty()) {
        item {
          Text(
            text = "My collection by expansion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
        items(state.expansions, key = { it.id }) { expansion ->
          ExpansionProgressCard(expansion = expansion)
        }
      } else if (!state.isLoading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "No cards in your collection yet.\nAdd cards from the browser!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
    } // Box
  }
}

@Composable
private fun CollectionStatCard(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ExpansionProgressCard(
  expansion: ExpansionProgress,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = expansion.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "${expansion.ownedCards} / ${expansion.totalCards}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      CollectionBar(
        count = expansion.ownedCards,
        total = expansion.totalCards.coerceAtLeast(1),
      )
    }
  }
}

@CircuitInject(MergeActivityScope::class, CollectionScreen::class)
@Inject
class CollectionPresenter(
  @Assisted private val navigator: Navigator,
  private val collectionRepository: CollectionRepository,
  private val expansionsRepository: ExpansionsRepository,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<CollectionUiState> {
  @Composable
  override fun present(): CollectionUiState {
    var dailyRewardClaimed by rememberSaveable { mutableStateOf(false) }
    var rewardMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val collectionData by remember {
      combine(
        collectionRepository.observeCollection(),
        expansionsRepository.observeExpansions(),
      ) { collection, expansions ->
        val progressList = expansions
          .map { expansion ->
            ExpansionProgress(
              id = expansion.id,
              name = expansion.name,
              symbolUrl = expansion.images.symbol,
              ownedCards = collection[expansion.id],
              totalCards = expansion.printedTotal,
            )
          }
          .filter { it.ownedCards > 0 }
          .sortedByDescending { it.ownedCards }
        Triple(progressList.sumOf { it.ownedCards }, progressList.size, progressList)
      }
    }.collectAsState(Triple(0, 0, emptyList()))

    val (totalCards, uniqueExpansions, expansions) = collectionData

    return CollectionUiState(
      totalCards = totalCards,
      uniqueExpansions = uniqueExpansions,
      expansions = expansions,
      isLoading = false,
      dailyRewardClaimed = dailyRewardClaimed,
      rewardMessage = rewardMessage,
      eventSink = { event ->
        when (event) {
          CollectionUiEvent.ClaimDailyReward -> {
            if (!dailyRewardClaimed) {
              val token = settings.authToken
              if (token != null) {
                scope.launch {
                  backendApi.claimDailyReward(token).fold(
                    onSuccess = { gold ->
                      dailyRewardClaimed = true
                      settings.goldBalance = settings.goldBalance + gold
                      rewardMessage = "Récompense journalière réclamée ! +$gold pièces d'or."
                    },
                    onFailure = { t ->
                      rewardMessage = t.message ?: "Erreur lors de la réclamation."
                    },
                  )
                }
              } else {
                dailyRewardClaimed = true
                settings.goldBalance = settings.goldBalance + 10
                rewardMessage = "Récompense journalière réclamée ! +10 pièces d'or."
              }
            }
          }
          CollectionUiEvent.OpenSettings -> navigator.goTo(SettingsScreen())
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(MergeActivityScope::class, ShopScreen::class)
@Composable
internal fun Shop(
  state: ShopUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = modifier,
    topBar = {
      DeckBoxRootAppBar(
        title = LocalStrings.current.shop,
        actions = {
          IconButton(onClick = { eventSink(ShopUiEvent.OpenSettings) }) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = WindowInsets.systemBars,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxWidth()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(28.dp))
          Column {
            Text("Gold Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            Text("${state.goldBalance} coins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
          }
        }
      }

      ElevatedButton(
        onClick = { eventSink(ShopUiEvent.BuyPremiumPass) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = !state.premiumPassPurchased,
      ) {
        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = if (state.premiumPassPurchased) "✓ Premium Pass Active" else "Buy Premium Pass — 500 gold",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      state.purchaseMessage?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium)
      }

      HorizontalDivider()

      Text(
        text = "Featured items",
        style = MaterialTheme.typography.titleMedium,
      )

      state.shopItems.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth()) {
          ListItem(
            headlineContent = { Text(text = item.name) },
            supportingContent = { Text(text = item.description) },
            trailingContent = {
              Button(onClick = { eventSink(ShopUiEvent.BuyItem(item)) }) {
                Text(text = "${item.cost} gold")
              }
            },
          )
        }
      }
    }
    } // Box
  }
}

@CircuitInject(MergeActivityScope::class, ShopScreen::class)
@Inject
class ShopPresenter(
  @Assisted private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<ShopUiState> {
  @Composable
  override fun present(): ShopUiState {
    var purchaseMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val goldBalance by remember { settings.observeGoldBalance() }.collectAsState(1200)
    val premiumPassPurchased by remember { settings.observePremiumPassActive() }.collectAsState(false)
    val scope = rememberCoroutineScope()
    val items = remember {
      listOf(
        ShopItem("Booster Bundle", 400, "Get 5 booster packs and bonus coins."),
        ShopItem("Trainer Kit", 250, "A set of trainer cards for your deck."),
        ShopItem("Premium Sleeve", 150, "Show off rare art on your deck sleeve."),
      )
    }

    return ShopUiState(
      goldBalance = goldBalance,
      premiumPassPurchased = premiumPassPurchased,
      purchaseMessage = purchaseMessage,
      shopItems = items,
      eventSink = { event ->
        when (event) {
          is ShopUiEvent.BuyItem -> {
            if (goldBalance >= event.item.cost) {
              settings.goldBalance = goldBalance - event.item.cost
              purchaseMessage = "Acheté : ${event.item.name} !"
            } else {
              purchaseMessage = "Or insuffisant pour ${event.item.name}."
            }
          }
          ShopUiEvent.BuyPremiumPass -> {
            if (!premiumPassPurchased && goldBalance >= 500) {
              val token = settings.authToken
              if (token != null) {
                scope.launch {
                  backendApi.buyPremiumPass(token).fold(
                    onSuccess = {
                      settings.goldBalance = goldBalance - 500
                      settings.premiumPassActive = true
                      purchaseMessage = "Pass Premium activé pour le mois !"
                    },
                    onFailure = { t ->
                      purchaseMessage = t.message ?: "Achat échoué."
                    },
                  )
                }
              } else {
                settings.goldBalance = goldBalance - 500
                settings.premiumPassActive = true
                purchaseMessage = "Pass Premium activé (hors ligne)."
              }
            } else if (premiumPassPurchased) {
              purchaseMessage = "Le Pass Premium est déjà actif."
            } else {
              purchaseMessage = "Vous avez besoin de 500 pièces d'or."
            }
          }
          ShopUiEvent.OpenSettings -> navigator.goTo(SettingsScreen())
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(MergeActivityScope::class, ProfileScreen::class)
@Composable
internal fun Profile(
  state: ProfileUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  val overlayHost = LocalOverlayHost.current
  val coroutineScope = rememberCoroutineScope()
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = modifier,
    topBar = {
      DeckBoxRootAppBar(
        title = LocalStrings.current.profile,
        actions = {
          IconButton(onClick = { eventSink(ProfileUiEvent.OpenSettings) }) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = WindowInsets.systemBars,
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 860.dp)
        .fillMaxWidth()
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Profile hero card
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier.padding(20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Avatar circle
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = state.selectedAvatar,
                style = MaterialTheme.typography.headlineMedium,
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = state.playerName,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Rank #${state.rank} · ${state.totalWins} wins",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
          }
          IconButton(onClick = { eventSink(ProfileUiEvent.EditProfile) }) {
            Icon(
              Icons.Filled.Edit,
              contentDescription = "Edit profile",
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      // Stats row
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileStatCard(
          icon = Icons.Filled.AttachMoney,
          label = "Gold",
          value = "${state.goldBalance}",
          modifier = Modifier.weight(1f),
        )
        ProfileStatCard(
          icon = Icons.Filled.Collections,
          label = "Cards",
          value = "${state.totalCards}",
          modifier = Modifier.weight(1f),
        )
        ProfileStatCard(
          icon = Icons.Filled.Star,
          label = "Pass",
          value = if (state.premiumPassActive) "Active" else "Inactive",
          modifier = Modifier.weight(1f),
        )
      }

      HorizontalDivider()

      Button(
        onClick = {
          coroutineScope.launch {
            overlayHost.showBottomSheetScreen(FriendsScreen())
          }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = LocalStrings.current.viewFriends, style = MaterialTheme.typography.titleMedium)
      }

      // Language selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = LocalStrings.current.languageSelectorLabel,
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
        )
        FilterChip(
          selected = state.appLanguage == DeckBoxSettings.AppLanguage.FRENCH,
          onClick = { eventSink(ProfileUiEvent.SetLanguage(DeckBoxSettings.AppLanguage.FRENCH)) },
          label = { Text(LocalStrings.current.languageFrench) },
        )
        FilterChip(
          selected = state.appLanguage == DeckBoxSettings.AppLanguage.ENGLISH,
          onClick = { eventSink(ProfileUiEvent.SetLanguage(DeckBoxSettings.AppLanguage.ENGLISH)) },
          label = { Text(LocalStrings.current.languageEnglish) },
        )
      }

      if (state.isLoggedIn) {
        OutlinedButton(
          onClick = { eventSink(ProfileUiEvent.Logout) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(LocalStrings.current.logout, style = MaterialTheme.typography.titleMedium)
        }
      }
    }
    } // Box

    // Edit profile dialog
    if (state.showEditDialog) {
      EditProfileDialog(
        currentName = state.playerName,
        currentAvatar = state.selectedAvatar,
        onSave = { name, avatar -> eventSink(ProfileUiEvent.SaveProfile(name, avatar)) },
        onDismiss = { eventSink(ProfileUiEvent.DismissEdit) },
      )
    }
  }
}

@Composable
private fun ProfileStatCard(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun EditProfileDialog(
  currentName: String,
  currentAvatar: String,
  onSave: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(currentName) }
  var selectedAvatar by remember { mutableStateOf(currentAvatar) }
  val avatars = listOf("🎮", "⚡", "🔥", "💧", "🌿", "🏆", "⭐", "🎯", "🐉", "🦊")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Profile") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Player Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Choose Avatar", style = MaterialTheme.typography.labelLarge)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          avatars.take(5).forEach { emoji ->
            AvatarOption(
              emoji = emoji,
              selected = emoji == selectedAvatar,
              onClick = { selectedAvatar = emoji },
            )
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          avatars.drop(5).forEach { emoji ->
            AvatarOption(
              emoji = emoji,
              selected = emoji == selectedAvatar,
              onClick = { selectedAvatar = emoji },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSave(name.trim().ifEmpty { currentName }, selectedAvatar) }) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun AvatarOption(emoji: String, selected: Boolean, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.size(40.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(emoji, style = MaterialTheme.typography.titleMedium)
    }
  }
}

@CircuitInject(MergeActivityScope::class, ProfileScreen::class)
@Inject
class ProfilePresenter(
  @Assisted private val navigator: Navigator,
  private val collectionRepository: CollectionRepository,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<ProfileUiState> {
  @Composable
  override fun present(): ProfileUiState {
    val totalCards by remember {
      collectionRepository.observeCollection().map { it.totalCount }
    }.collectAsState(0)
    val playerName by remember { settings.observePlayerName() }.collectAsState("Trainer")
    val goldBalance by remember { settings.observeGoldBalance() }.collectAsState(1200)
    val premiumPassActive by remember { settings.observePremiumPassActive() }.collectAsState(false)
    val authToken by remember { settings.observeAuthToken() }.collectAsState(settings.authToken)
    val appLanguage by remember { settings.observeAppLanguage() }.collectAsState(settings.appLanguage)
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var selectedAvatar by rememberSaveable { mutableStateOf("🎮") }

    return ProfileUiState(
      playerName = playerName,
      rank = 0,
      totalWins = 0,
      totalCards = totalCards,
      goldBalance = goldBalance,
      premiumPassActive = premiumPassActive,
      isLoggedIn = authToken != null,
      appLanguage = appLanguage,
      showEditDialog = showEditDialog,
      selectedAvatar = selectedAvatar,
      eventSink = { event ->
        when (event) {
          ProfileUiEvent.ViewFriends -> Unit
          ProfileUiEvent.OpenSettings -> navigator.goTo(SettingsScreen())
          ProfileUiEvent.EditProfile -> { showEditDialog = true }
          is ProfileUiEvent.SaveProfile -> {
            settings.playerName = event.name
            selectedAvatar = event.avatar
            showEditDialog = false
          }
          ProfileUiEvent.DismissEdit -> { showEditDialog = false }
          ProfileUiEvent.Logout -> {
            settings.authToken = null
            settings.userId = null
            navigator.goTo(LoginScreen())
          }
          is ProfileUiEvent.SetLanguage -> {
            settings.appLanguage = event.language
          }
        }
      },
    )
  }
}
