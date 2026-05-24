package app.deckbox.backend

import app.deckbox.backend.database.*
import app.deckbox.backend.seeding.CardSeeder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val database = Database.connect(
            url = "jdbc:h2:file:./deckboxdb",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Cards, Decks, Battles, Trades, Friends, ShopItems, UserInventory, PasswordResets,
            )
        }

        CardSeeder.seed()
    }
}

fun Application.configureDatabases() {
    DatabaseFactory.init()
}
