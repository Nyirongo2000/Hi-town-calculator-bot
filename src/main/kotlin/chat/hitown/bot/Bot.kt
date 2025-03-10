/**
 * Write your bot here!
 *
 * Also see `Models.kt` for additional information.
 */

package chat.hitown.bot

import chat.hitown.bot.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import java.io.File
import java.util.*

private const val INSTALL_SECRET = "hitownbot"

@Serializable
data class GroupInstall(
    val groupId: String,
    val groupName: String,
    val webhook: String,
    val config: List<BotConfigValue> = emptyList(),
    val isPaused: Boolean = false
)

/**
 * Bot instance.
 */
val bot = Bot()

class Bot {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private val groupInstalls = mutableMapOf<String, GroupInstall>()
    private val saveFile = File("./bot_state.json")
    private val calculator = Calculator()

    private var _messagesProcessed = 0
    private var _errorCount = 0
    private val startTime = System.currentTimeMillis()

    val groupCount: Int
        get() = groupInstalls.size

    val messagesProcessed: Int
        get() = _messagesProcessed

    val errorCount: Int
        get() = _errorCount

    val uptime: Long
        get() = System.currentTimeMillis() - startTime

    init {
        println("=== Bot Initialization ===")
        loadState()
        println("Current group installs: ${groupInstalls.size}")
        groupInstalls.forEach { (token, install) ->
            println("Group: ${install.groupName} (${install.groupId})")
            println("  Token: $token")
            println("  Webhook: ${install.webhook}")
            println("  Config: ${install.config}")
            println("  Paused: ${install.isPaused}")
        }
    }

    private fun loadState() {
        try {
            println("Loading state from file: ${saveFile.absolutePath}")
            if (saveFile.exists()) {
                val state = Json.decodeFromString<Map<String, GroupInstall>>(saveFile.readText())
                groupInstalls.clear()
                groupInstalls.putAll(state)
                println("Loaded ${groupInstalls.size} group installs")
            } else {
                println("No state file found, starting fresh")
            }
        } catch (e: Exception) {
            println("Error loading state: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun saveState() {
        try {
            println("Saving state to file: ${saveFile.absolutePath}")
            saveFile.writeText(Json.encodeToString(groupInstalls))
            println("Saved ${groupInstalls.size} group installs")
        } catch (e: Exception) {
            println("Error saving state: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Bot details as shown in Hi Town.
     */
    val details = BotDetails(
        /**
         * Bot name.
         */
        name = "Hi-Town-Calculator-Bot",
        /**
         * Bot description.
         */
        description = "A calculator bot that can handle a wide range of mathematical computations. Use @calc [expression] to perform calculations.",
        /**
         * Keywords that will cause a Hi Town group message to be sent to the bot.
         */
        keywords = listOf("@calc"),
        /**
         * Available configuration options for the bot (optional).
         */
        config = emptyList()
    )

    fun validateInstall(secret: String?): Boolean {
        println("=== Validating Install Secret ===")
        println("Received secret: $secret")
        println("Expected secret: $INSTALL_SECRET")
        return secret == INSTALL_SECRET
    }

    fun install(token: String, body: InstallBotBody) {
        println("=== Installing Bot ===")
        println("Token: $token")
        println("Group ID: ${body.groupId}")
        println("Group Name: ${body.groupName}")
        println("Webhook: ${body.webhook}")
        println("Config: ${body.config}")
        
        try {
            val config = body.config?.toList() ?: emptyList()
            println("Using config: $config")
            
            groupInstalls[token] = GroupInstall(
                groupId = body.groupId,
                groupName = body.groupName,
                webhook = body.webhook,
                config = config,
                isPaused = false
            )
            saveState()
            println("Bot installed successfully in group: ${body.groupName}")
            println("Current group installs: ${groupInstalls.size}")
        } catch (e: Exception) {
            println("Error installing bot: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun reinstall(token: String, config: List<BotConfigValue>?) {
        println("=== Reinstalling Bot ===")
        println("Token: $token")
        println("New config: $config")
        
        groupInstalls[token]?.let { install ->
            println("Found existing install for group: ${install.groupName}")
            groupInstalls[token] = install.copy(config = config ?: emptyList())
            saveState()
            println("Bot reinstalled successfully")
        } ?: run {
            println("No existing install found for token: $token")
        }
    }

    fun uninstall(token: String) {
        println("=== Uninstalling Bot ===")
        println("Token: $token")
        
        groupInstalls[token]?.let { install ->
            println("Found install for group: ${install.groupName}")
            groupInstalls.remove(token)
            saveState()
            println("Bot uninstalled successfully")
        } ?: run {
            println("No install found for token: $token")
        }
    }

    fun pause(token: String) {
        println("=== Pausing Bot ===")
        println("Token: $token")
        
        groupInstalls[token]?.let { install ->
            println("Found install for group: ${install.groupName}")
            groupInstalls[token] = install.copy(isPaused = true)
            saveState()
            println("Bot paused successfully")
        } ?: run {
            println("No install found for token: $token")
        }
    }

    fun resume(token: String) {
        println("=== Resuming Bot ===")
        println("Token: $token")
        
        groupInstalls[token]?.let { install ->
            println("Found install for group: ${install.groupName}")
            groupInstalls[token] = install.copy(isPaused = false)
            saveState()
            println("Bot resumed successfully")
        } ?: run {
            println("No install found for token: $token")
        }
    }

    /**
     * Handle messages sent to the Hi Town group.
     */
    fun message(token: String, body: MessageBotBody): MessageBotResponse {
        println("=== Processing Message ===")
        println("Token: $token")
        println("Message: ${body.message}")
        println("Person: ${body.person}")

        val install = groupInstalls[token] ?: run {
            println("No install found for token: $token")
            return MessageBotResponse(success = false, note = "Bot not installed")
        }

        if (install.isPaused) {
            println("Bot is paused")
            return MessageBotResponse(success = true)
        }

        val message = body.message ?: return MessageBotResponse(success = false, note = "No message provided")

        // Check if the message starts with @calc
        if (!message.trim().startsWith("@calc")) {
            return MessageBotResponse(success = true)
        }

        // Extract the expression after @calc
        val expression = message.trim().substringAfter("@calc").trim()
        if (expression.isEmpty()) {
            return MessageBotResponse(
                success = false,
                note = "Please provide an expression to calculate",
                actions = listOf(BotAction(message = "Usage: @calc [expression]\nExample: @calc 2 + 2"))
            )
        }

        return try {
            val result = calculator.evaluate(expression)
            // Format the result to a reasonable number of decimal places
            val formattedResult = when {
                result.isInfinite() -> "Infinity"
                result.isNaN() -> "Not a number"
                result % 1 == 0.0 -> result.toLong().toString()
                else -> "%.6f".format(result).trimEnd('0').trimEnd('.')
            }
            MessageBotResponse(
                success = true,
                actions = listOf(BotAction(message = formattedResult))
            )
        } catch (e: IllegalArgumentException) {
            MessageBotResponse(
                success = false,
                note = e.message ?: "Invalid expression",
                actions = listOf(BotAction(message = "Error: ${e.message}"))
            )
        } catch (e: Exception) {
            println("Error processing message: ${e.message}")
            e.printStackTrace()
            MessageBotResponse(
                success = false,
                note = "An error occurred while processing your request",
                actions = listOf(BotAction(message = "An error occurred. Please check your expression and try again."))
            )
        }
    }

    /**
     * Sends a message to a group's webhook.
     * @param token The group's token
     * @param messages List of messages to send
     * @return true if the message was sent successfully, false if the bot is paused
     * @throws Exception if there's an error sending the message
     */
    suspend fun sendWebhookMessage(token: String, messages: List<String>): Boolean {
        val install = groupInstalls[token] ?: throw IllegalArgumentException("Bot not installed")
        
        if (install.isPaused) {
            return false
        }

        try {
            client.post(install.webhook) {
                contentType(ContentType.Application.Json)
                setBody(messages)
            }
            return true
        } catch (e: Exception) {
            println("Error sending webhook message: ${e.message}")
            throw e
        }
    }
}
