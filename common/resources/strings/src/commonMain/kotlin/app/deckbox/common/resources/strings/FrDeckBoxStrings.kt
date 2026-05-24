package app.deckbox.common.resources.strings

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.deckbox.core.model.Format
import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = "fr")
val FrDeckBoxStrings = DeckBoxStrings(
  standardLegality = "Standard",
  expandedLegality = "Étendu",
  unlimitedLegality = "Illimité",
  genericEmptyCardsMessage = "Un Ronflex bloque le passage.\nEssayez un autre chemin.",
  genericSearchEmpty = { query ->
    buildAnnotatedString {
      append("Aucun résultat pour ")
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append("\"${query ?: "???"}\"")
      }
      append(".")
      append("\nVous vous êtes blessé dans la confusion.")
    }
  },
  cardPlaceholderContentDescription = "Carte Pokémon",
  refreshPricesContentDescription = "Actualiser les prix",
  format = { format ->
    when (format) {
      Format.STANDARD -> "Standard"
      Format.EXPANDED -> "Étendu"
      Format.UNLIMITED -> "Illimité"
      Format.LEGACY -> "Héritage"
      Format.THEME -> "Thème"
    }
  },

  decks = "Decks",
  decksTabContentDescription = "Liste des decks sauvegardés",
  deckDefaultNoName = "Un deck sans nom",
  deckLastUpdated = { timestamp -> "Mis à jour $timestamp" },
  deckActionTestButton = "Tester",
  deckActionDuplicateButton = "Dupliquer",
  deckActionDuplicateButtonContentDescription = "Dupliquer le deck",
  deckActionDeleteButton = "Supprimer",
  deckActionDeleteButtonContentDescription = "Supprimer le deck",
  fabActionNewDeckButton = "Nouveau deck",
  addSuggestedEnergyCards = { count, name -> "Ajouter $count cartes $name au deck" },
  importTournaments = "Tournois",
  importText = "Texte",
  importTextTitle = "Importer du texte",

  boosterPacks = "Boosters",
  boosterPacksTitleLong = "Boosters",
  boosterPacksTabContentDescription = "Liste des boosters personnalisés",
  boosterPackTitleNoName = "Nom du booster",

  expansions = "Extensions",
  expansionsTabContentDescription = "Liste des extensions",
  expansionReleaseDate = { "Sortie le $it" },
  collection = "Collection",
  inCollection = "dans la collection",
  collectionCountOfTotal = { count, total -> "$count sur $total" },
  expansionSearchHint = "Rechercher une extension",
  expansionSearchEmptyMessage = { "Aucune extension trouvée pour $it" },
  expansionsEmptyMessage = "Aucune extension trouvée.",
  expansionsErrorMessage = "Impossible de charger les extensions.",
  fabActionEditCollection = "Modifier la collection",
  collectionEditingTitle = "Modification de la collection",

  browse = "Parcourir",
  browseTabContentDescription = "Parcourir toutes les cartes Pokémon",
  browseSearchHint = "Rechercher une carte",
  battle = "Combat",
  battleTabContentDescription = "Entrer dans le lobby de combat",
  collectionTabContentDescription = "Voir votre collection de cartes",
  shop = "Boutique",
  shopTabContentDescription = "Visiter la boutique",
  profile = "Profil",
  profileTabContentDescription = "Ouvrir votre profil",
  profileOverview = "Aperçu du profil",
  viewFriends = "Voir les amis",
  goldBalance = "Solde d'or",
  premiumPassStatusActive = "Pass Premium actif",
  premiumPassStatusInactive = "Pass Premium inactif",
  collectionSize = "Taille de la collection",
  friends = "Amis",
  friendRequests = "Demandes d'amis",
  suggestedFriends = "Amis suggérés",
  accept = "Accepter",
  decline = "Refuser",
  close = "Fermer",
  pending = "En attente",
  accepted = "Accepté",
  declined = "Refusé",

  tcgPlayer = "TCGPlayer",
  tcgPlayerNormal = "Normal",
  tcgPlayerHolofoil = "Holo",
  tcgPlayerReverseHolofoil = "Reverse Holo",
  tcgPlayerFirstEditionHolofoil = "1ère Édition Holo",
  tcgPlayerFirstEditionNormal = "1ère Édition",
  priceMarket = "Marché",
  priceLow = "Bas",
  priceMid = "Moyen",
  priceHigh = "Élevé",
  cardMarket = "Cardmarket",
  priceTrend = "Tendance",
  oneDayAvg = "Moy. 1 jour",
  sevenDayAvg = "Moy. 7 jours",
  thirtyDayAvg = "Moy. 30 jours",
  actionBuy = "Acheter",

  lessThan = { "Moins de $it" },
  lessThanEqual = { "Moins ou égal à $it" },
  greaterThan = { "Plus de $it" },
  greaterThanEqual = { "Plus ou égal à $it" },

  tournamentsTitle = "Tournois",
  tournamentsErrorMessage = "Impossible de charger les tournois récents",
  fabActionImport = "Importer un deck",

  settings = "Paramètres",
  settingsTabContentDescription = "Modifier les paramètres",
  decksEmptyStateMessage = "Vous n'avez pas encore de deck !\nEssayez d'en créer un ou d'importer un deck existant.",
  deckListHeaderPokemon = "Pokémon",
  deckListHeaderTrainer = "Dresseur",
  deckListHeaderEnergy = "Énergie",
  cardCountInDeck = { count ->
    if (count == 1) "$count Copie" else "$count Copies"
  },
  deckTitleNoName = "Entrez un nom pour votre deck",
  fabActionNewBoosterPack = "Nouveau booster",
  actionCancel = "Annuler",
  actionDelete = "Supprimer",
  actionDeleteAreYouSure = "Êtes-vous sûr ?",
  boosterPickerEmptyMessage = "Vous n'avez pas encore de booster. Créez-en pour les ajouter à vos decks.",
  boosterPackNoName = "Booster sans nom",
  deckNoName = "Deck sans nom",
  deckPickerTitle = "Choisir un deck",
  boosterPackPickerTitle = "Choisir un booster",

  timeAgoNow = "maintenant",
  timeAgoMinutes = { min -> "il y a $min minutes" },
  timeAgoHours = { hrs -> "il y a $hrs heures" },
  timeAgoDays = { days -> "il y a $days jours" },
  timeAgoMonths = { months -> "il y a $months mois" },
  timeAgoYears = { yrs -> "il y a $yrs ans" },
  deckSortOrderUpdatedAt = "Dernière mise à jour",
  deckSortOrderCreatedAt = "Créé",
  deckSortOrderAlphabetically = "Alphabétiquement",
  deckSortOrderLegality = "Légalité",
  gridStyleCompact = "Compact",
  gridStyleSmall = "Petit",
  gridStyleLarge = "Grand",
  favorites = "Favoris",
  similarCardsLabel = "Cartes similaires",
  evolvesFromLabel = "Évolue depuis",
  evolvesToLabel = "Évolue vers",
  similarCardsErrorLabel = "Impossible de charger les cartes similaires",
  similarCardsEmptyLabel = "Aucune carte similaire trouvée",
  evolvesFromErrorLabel = "Impossible de charger les cartes précédentes",
  evolvesFromEmptyLabel = "Aucune carte trouvée",
  evolvesToErrorLabel = "Impossible de charger les évolutions",
  evolvesToEmptyLabel = "Aucune évolution trouvée",
  cardDetailAddedToDeck = { name -> "Ajouté à \"$name\"" },
  cardDetailAddedToBoosterPack = { name ->
    name?.let { "Ajouté à \"$it\"" } ?: "Ajouté au booster"
  },
  builderEditingTitle = "Modification",
  deckPickerEmptyMessage = "Vous n'avez pas de deck. Créez-en un pour commencer.",

  loginTitle = "DeckBox",
  loginSubtitle = "Connectez-vous pour jouer en ligne",
  loginUsernameLabel = "Nom d'utilisateur",
  loginPasswordLabel = "Mot de passe",
  loginButton = "Se connecter",
  loginGoRegister = "Nouveau ? Créer un compte",
  loginPlayOffline = "Jouer hors ligne",
  loginForgotPassword = "Mot de passe oublié ?",

  registerTitle = "Créer un compte",
  registerSubtitle = "Rejoignez la communauté DeckBox",
  registerEmailLabel = "Email",
  registerCreateButton = "Créer le compte",
  registerGoLogin = "Déjà un compte ? Se connecter",

  forgotPasswordTitle = "Mot de passe oublié",
  forgotPasswordSubtitle = "Entrez votre email pour recevoir un code de réinitialisation",
  forgotPasswordEmailLabel = "Adresse email",
  forgotPasswordSendButton = "Envoyer le code",
  forgotPasswordCodeHint = "Entrez le code reçu",
  forgotPasswordNewPasswordLabel = "Nouveau mot de passe",
  forgotPasswordResetButton = "Réinitialiser le mot de passe",
  forgotPasswordCodeSent = { code -> "Votre code de réinitialisation est : $code\n(Valide 15 minutes)" },
  forgotPasswordGoLogin = "Retour à la connexion",

  languageSelectorLabel = "Langue",
  languageEnglish = "English",
  languageFrench = "Français",

  logout = "Se déconnecter",
)
