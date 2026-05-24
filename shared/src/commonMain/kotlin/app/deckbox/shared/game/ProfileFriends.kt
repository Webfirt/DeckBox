package app.deckbox.shared.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deckbox.common.compose.DeckBoxRootAppBar
import app.deckbox.common.screens.FriendsScreen
import app.deckbox.common.settings.DeckBoxSettings
import app.deckbox.core.di.MergeActivityScope
import app.deckbox.shared.auth.BackendApiService
import cafe.adriel.lyricist.LocalStrings
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

data class FriendRequest(
  val id: String,
  val playerName: String,
  val note: String,
  val status: FriendRequestStatus,
)

enum class FriendRequestStatus {
  Pending,
  Accepted,
  Declined,
}

data class FriendsUiState(
  val requests: List<FriendRequest>,
  val suggestions: List<String>,
  val eventSink: (FriendsUiEvent) -> Unit,
) : CircuitUiState

sealed interface FriendsUiEvent {
  data class Accept(val requestId: String) : FriendsUiEvent
  data class Decline(val requestId: String) : FriendsUiEvent
  data class AddFriend(val playerName: String) : FriendsUiEvent
  data object Close : FriendsUiEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(MergeActivityScope::class, FriendsScreen::class)
@Composable
internal fun Friends(
  state: FriendsUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = modifier,
    topBar = {
      DeckBoxRootAppBar(
        title = LocalStrings.current.friends,
        actions = {
          IconButton(onClick = { eventSink(FriendsUiEvent.Close) }) {
            Icon(Icons.Filled.Close, contentDescription = null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .padding(paddingValues)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = LocalStrings.current.friendRequests,
        style = MaterialTheme.typography.headlineMedium,
      )

      Text(
        text = "Manage pending requests and invited friends.",
        style = MaterialTheme.typography.bodyLarge,
      )

      state.requests.forEach { request ->
        Card(
          modifier = Modifier.fillMaxWidth(),
        ) {
          ListItem(
            headlineContent = { Text(text = request.playerName) },
            supportingContent = { Text(text = request.note) },
            leadingContent = {
              Icon(
                imageVector = when (request.status) {
                  FriendRequestStatus.Pending -> Icons.Filled.PersonAdd
                  FriendRequestStatus.Accepted -> Icons.Filled.CheckCircle
                  FriendRequestStatus.Declined -> Icons.Filled.PersonOff
                },
                contentDescription = null,
              )
            },
            trailingContent = {
              if (request.status == FriendRequestStatus.Pending) {
                Column(
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Button(onClick = { eventSink(FriendsUiEvent.Accept(request.id)) }) {
                    Text(text = LocalStrings.current.accept)
                  }
                  Button(onClick = { eventSink(FriendsUiEvent.Decline(request.id)) }) {
                    Text(text = LocalStrings.current.decline)
                  }
                }
              } else {
                Text(
                  text = when (request.status) {
                    FriendRequestStatus.Pending -> LocalStrings.current.pending
                    FriendRequestStatus.Accepted -> LocalStrings.current.accepted
                    FriendRequestStatus.Declined -> LocalStrings.current.declined
                  },
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            },
          )
        }
      }

      Text(
        text = LocalStrings.current.suggestedFriends,
        style = MaterialTheme.typography.titleMedium,
      )

      state.suggestions.forEach { suggestion ->
        Card(
          modifier = Modifier.fillMaxWidth(),
        ) {
          ListItem(
            headlineContent = { Text(text = suggestion) },
            supportingContent = { Text(text = "Suggested match for your collection.") },
            trailingContent = {
              Button(onClick = { eventSink(FriendsUiEvent.AddFriend(suggestion)) }) {
                Text(text = "Add")
              }
            },
          )
        }
      }

      Button(
        onClick = { eventSink(FriendsUiEvent.Close) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = LocalStrings.current.close)
      }
    }
  }
}

@CircuitInject(MergeActivityScope::class, FriendsScreen::class)
@Inject
class FriendsPresenter(
  @Assisted private val navigator: Navigator,
  private val settings: DeckBoxSettings,
  private val backendApi: BackendApiService,
) : Presenter<FriendsUiState> {
  @Composable
  override fun present(): FriendsUiState {
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var suggestions by remember { mutableStateOf(listOf("PikachuPal", "CharizardChamp", "EeveeExpert")) }

    val token = settings.authToken
    LaunchedEffect(token) {
      if (token != null) {
        backendApi.getFriends(token).onSuccess { friends ->
          val loaded = friends.map { f ->
            FriendRequest(id = f.id, playerName = f.username, note = "Ami confirmé", status = FriendRequestStatus.Accepted)
          }
          if (loaded.isNotEmpty()) requests = loaded
        }
      }
      if (requests.isEmpty()) {
        requests = listOf(
          FriendRequest("req-1", "MistyMaverick", "Veut échanger des cartes Eau.", FriendRequestStatus.Pending),
          FriendRequest("req-2", "BrockBeast", "Invitation pour des matchs classés.", FriendRequestStatus.Pending),
        )
      }
    }

    return FriendsUiState(
      requests = requests,
      suggestions = suggestions,
      eventSink = { event ->
        when (event) {
          is FriendsUiEvent.Accept -> {
            requests = requests.map {
              if (it.id == event.requestId) it.copy(status = FriendRequestStatus.Accepted) else it
            }
          }
          is FriendsUiEvent.Decline -> {
            requests = requests.map {
              if (it.id == event.requestId) it.copy(status = FriendRequestStatus.Declined) else it
            }
          }
          is FriendsUiEvent.AddFriend -> {
            scope.launch {
              if (token != null) {
                backendApi.sendFriendRequest(token, event.playerName)
              }
              suggestions = suggestions - event.playerName
              requests = requests + FriendRequest(
                id = "req-${requests.size + 1}",
                playerName = event.playerName,
                note = "Demande d'ami envoyée.",
                status = FriendRequestStatus.Pending,
              )
            }
          }
          FriendsUiEvent.Close -> navigator.pop()
        }
      },
    )
  }
}
