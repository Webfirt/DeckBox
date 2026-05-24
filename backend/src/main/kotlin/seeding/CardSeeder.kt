package app.deckbox.backend.seeding

import app.deckbox.backend.database.Cards
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object CardSeeder {
    private data class CardData(val id: String, val name: String, val type: String, val rarity: String)

    private val starterCards = listOf(
        CardData("pikachu-base1-58", "Pikachu", "Lightning", "Common"),
        CardData("raichu-base1-14", "Raichu", "Lightning", "Uncommon"),
        CardData("jolteon-base1-4", "Jolteon", "Lightning", "Rare Holo"),
        CardData("charmander-base1-46", "Charmander", "Fire", "Common"),
        CardData("charmeleon-base1-24", "Charmeleon", "Fire", "Uncommon"),
        CardData("charizard-base1-4", "Charizard", "Fire", "Rare Holo"),
        CardData("magmar-base1-36", "Magmar", "Fire", "Uncommon"),
        CardData("squirtle-base1-63", "Squirtle", "Water", "Common"),
        CardData("wartortle-base1-42", "Wartortle", "Water", "Uncommon"),
        CardData("blastoise-base1-2", "Blastoise", "Water", "Rare Holo"),
        CardData("psyduck-base1-53", "Psyduck", "Water", "Common"),
        CardData("bulbasaur-base1-44", "Bulbasaur", "Grass", "Common"),
        CardData("ivysaur-base1-30", "Ivysaur", "Grass", "Uncommon"),
        CardData("venusaur-base1-15", "Venusaur", "Grass", "Rare Holo"),
        CardData("oddish-base1-52", "Oddish", "Grass", "Common"),
        CardData("mewtwo-base1-10", "Mewtwo", "Psychic", "Rare Holo"),
        CardData("gengar-base1-5", "Gengar", "Psychic", "Rare Holo"),
        CardData("haunter-base1-22", "Haunter", "Psychic", "Uncommon"),
        CardData("gastly-base1-50", "Gastly", "Psychic", "Common"),
        CardData("jynx-base1-31", "Jynx", "Psychic", "Uncommon"),
        CardData("machamp-base1-8", "Machamp", "Fighting", "Rare Holo"),
        CardData("machoke-base1-34", "Machoke", "Fighting", "Uncommon"),
        CardData("machop-base1-52b", "Machop", "Fighting", "Common"),
        CardData("onix-base1-56", "Onix", "Fighting", "Common"),
        CardData("hitmonlee-base1-7", "Hitmonlee", "Fighting", "Rare Holo"),
        CardData("snorlax-base1-11", "Snorlax", "Colorless", "Rare Holo"),
        CardData("eevee-base1-51", "Eevee", "Colorless", "Common"),
        CardData("jigglypuff-base1-54", "Jigglypuff", "Colorless", "Common"),
        CardData("wigglytuff-base1-16", "Wigglytuff", "Colorless", "Rare Holo"),
        CardData("meowth-base1-55", "Meowth", "Colorless", "Common"),
    )

    fun seed() {
        transaction {
            starterCards.forEach { card ->
                if (Cards.select { Cards.id eq card.id }.empty()) {
                    Cards.insert {
                        it[Cards.id] = card.id
                        it[Cards.name] = card.name
                        it[Cards.type] = card.type
                        it[Cards.rarity] = card.rarity
                    }
                }
            }
        }
    }
}
