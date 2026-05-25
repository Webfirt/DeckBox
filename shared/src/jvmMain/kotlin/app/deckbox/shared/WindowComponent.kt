// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.deckbox.shared

import app.deckbox.common.screens.BattleScreen
import app.deckbox.common.screens.CollectionScreen
import app.deckbox.common.screens.ForgotPasswordScreen
import app.deckbox.common.screens.FriendsScreen
import app.deckbox.common.screens.LoginScreen
import app.deckbox.common.screens.MatchmakingScreen
import app.deckbox.common.screens.OnlineBattleScreen
import app.deckbox.common.screens.ProfileScreen
import app.deckbox.common.screens.RegisterScreen
import app.deckbox.common.screens.ShopScreen
import app.deckbox.common.settings.DeckBoxSettings
import app.deckbox.core.di.ActivityScope
import app.deckbox.core.di.MergeActivityScope
import app.deckbox.core.di.MergeAppScope
import app.deckbox.expansions.ExpansionsRepository
import app.deckbox.features.collection.api.CollectionRepository
import app.deckbox.shared.auth.BackendApiService
import app.deckbox.shared.auth.ForgotPassword
import app.deckbox.shared.auth.ForgotPasswordPresenter
import app.deckbox.shared.auth.ForgotPasswordUiState
import app.deckbox.shared.auth.Login
import app.deckbox.shared.auth.LoginPresenter
import app.deckbox.shared.auth.LoginUiState
import app.deckbox.shared.auth.Register
import app.deckbox.shared.auth.RegisterPresenter
import app.deckbox.shared.auth.RegisterUiState
import app.deckbox.shared.di.UiComponent
import app.deckbox.shared.game.Battle
import app.deckbox.shared.game.BattlePresenter
import app.deckbox.shared.game.BattleUiState
import app.deckbox.shared.game.Matchmaking
import app.deckbox.shared.game.MatchmakingPresenter
import app.deckbox.shared.game.MatchmakingUiState
import app.deckbox.shared.game.OnlineBattle
import app.deckbox.shared.game.OnlineBattlePresenter
import app.deckbox.shared.game.OnlineBattleUiState
import app.deckbox.shared.game.Collection
import app.deckbox.shared.game.CollectionPresenter
import app.deckbox.shared.game.CollectionUiState
import app.deckbox.shared.game.Friends
import app.deckbox.shared.game.FriendsPresenter
import app.deckbox.shared.game.FriendsUiState
import app.deckbox.shared.game.Profile
import app.deckbox.shared.game.ProfilePresenter
import app.deckbox.shared.game.ProfileUiState
import app.deckbox.shared.game.Shop
import app.deckbox.shared.game.ShopPresenter
import app.deckbox.shared.game.ShopUiState
import app.deckbox.shared.root.DeckBoxContentWithInsets
import com.r0adkll.kotlininject.merge.annotations.ContributesSubcomponent
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

@ActivityScope
@ContributesSubcomponent(
  scope = MergeActivityScope::class,
  parentScope = MergeAppScope::class,
)
abstract class WindowComponent : UiComponent {
  abstract val deckBoxContent: DeckBoxContentWithInsets
  abstract val collectionRepository: CollectionRepository
  abstract val expansionsRepository: ExpansionsRepository
  abstract val settings: DeckBoxSettings
  abstract val backendApiService: BackendApiService

  @Provides
  @IntoSet
  fun provideLoginPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is LoginScreen -> LoginPresenter(navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideLoginUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is LoginScreen -> ui<LoginUiState> { state, modifier -> Login(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideRegisterPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is RegisterScreen -> RegisterPresenter(navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideRegisterUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is RegisterScreen -> ui<RegisterUiState> { state, modifier -> Register(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideForgotPasswordPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is ForgotPasswordScreen -> ForgotPasswordPresenter(navigator, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideForgotPasswordUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is ForgotPasswordScreen -> ui<ForgotPasswordUiState> { state, modifier -> ForgotPassword(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideBattlePresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is BattleScreen -> BattlePresenter(navigator)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideBattleUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is BattleScreen -> ui<BattleUiState> { state, modifier -> Battle(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideCollectionPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is CollectionScreen -> CollectionPresenter(
        navigator,
        collectionRepository,
        expansionsRepository,
        settings,
        backendApiService,
      )
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideCollectionUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is CollectionScreen -> ui<CollectionUiState> { state, modifier -> Collection(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideShopPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is ShopScreen -> ShopPresenter(navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideShopUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is ShopScreen -> ui<ShopUiState> { state, modifier -> Shop(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideProfilePresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is ProfileScreen -> ProfilePresenter(navigator, collectionRepository, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideProfileUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is ProfileScreen -> ui<ProfileUiState> { state, modifier -> Profile(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideFriendsPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is FriendsScreen -> FriendsPresenter(navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideFriendsUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is FriendsScreen -> ui<FriendsUiState> { state, modifier -> Friends(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideMatchmakingPresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is MatchmakingScreen -> MatchmakingPresenter(screen, navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideMatchmakingUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is MatchmakingScreen -> ui<MatchmakingUiState> { state, modifier -> Matchmaking(state, modifier) }
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideOnlineBattlePresenterFactory(): Presenter.Factory = Presenter.Factory { screen, navigator, _ ->
    when (screen) {
      is OnlineBattleScreen -> OnlineBattlePresenter(screen, navigator, settings, backendApiService)
      else -> null
    }
  }

  @Provides
  @IntoSet
  fun provideOnlineBattleUiFactory(): Ui.Factory = Ui.Factory { screen, _ ->
    when (screen) {
      is OnlineBattleScreen -> ui<OnlineBattleUiState> { state, modifier -> OnlineBattle(state, modifier) }
      else -> null
    }
  }

  companion object
}
