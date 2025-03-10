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
            json()
        }
    }

    private val groupInstalls = mutableMapOf<String, GroupInstall>()
    private val saveFile = File("bot_state.json")
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

    init {
        loadState()
    }

    private fun loadState() {
        try {
            if (saveFile.exists()) {
                val state = Json.decodeFromString<Map<String, GroupInstall>>(saveFile.readText())
                groupInstalls.putAll(state)
            }
        } catch (e: Exception) {
            println("Error loading state: ${e.message}")
        }
    }

    private fun saveState() {
        try {
            saveFile.writeText(Json.encodeToString(groupInstalls))
        } catch (e: Exception) {
            println("Error saving state: ${e.message}")
        }
    }

    fun validateInstall(secret: String?): Boolean {
        val expectedSecret = System.getenv("BOT_SECRET")
        if (expectedSecret == null) {
            println("Warning: BOT_SECRET environment variable is not set")
            return false
        }
        return secret == expectedSecret
    }

    fun install(token: String, body: InstallBotBody) {
        groupInstalls[token] = GroupInstall(
            groupId = body.groupId,
            groupName = body.groupName,
            webhook = body.webhook,
            config = body.config ?: emptyList()
        )
        saveState()
    }

    fun reinstall(token: String, config: List<BotConfigValue>?) {
        groupInstalls[token]?.let { install ->
            groupInstalls[token] = install.copy(config = config ?: emptyList())
            saveState()
        }
    }

    fun uninstall(token: String) {
        groupInstalls.remove(token)
        saveState()
    }

    fun pause(token: String) {
        groupInstalls[token]?.let { install ->
            groupInstalls[token] = install.copy(isPaused = true)
            saveState()
        }
    }

    fun resume(token: String) {
        groupInstalls[token]?.let { install ->
            groupInstalls[token] = install.copy(isPaused = false)
            saveState()
        }
    }

    /**
     * Handle messages sent to the Hi Town group.
     */
    fun message(token: String, body: MessageBotBody): MessageBotResponse {
        val install = groupInstalls[token] ?: return MessageBotResponse(success = false, note = "Bot not installed")
        if (install.isPaused) return MessageBotResponse(success = true)

        val message = body.message ?: return MessageBotResponse(success = false, note = "No message provided")
        if (!message.trim().startsWith("@calc")) return MessageBotResponse(success = true)

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
        } catch (e: Exception) {
            MessageBotResponse(
                success = false,
                note = e.message ?: "Invalid expression",
                actions = listOf(BotAction(message = "Error: ${e.message}"))
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
