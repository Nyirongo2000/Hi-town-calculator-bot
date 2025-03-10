package chat.hitown.bot

import chat.hitown.bot.plugins.*
import kotlin.test.*

class BotTest {
    private val testBot = Bot()
    private val testToken = "test-token-123"
    private val testGroupId = "test-group-123"
    private val testGroupName = "Test Group"
    private val testWebhook = "https://api.hitown.chat/webhook/123"

    @Test
    fun `test bot details`() {
        val details = testBot.details
        assertNotNull(details.name)
        assertNotNull(details.description)
        assertTrue(details.keywords?.contains("@calc") == true)
        assertTrue(details.config?.isEmpty() == true)
    }

    @Test
    fun `test bot installation`() {
        val installBody = InstallBotBody(
            groupId = testGroupId,
            groupName = testGroupName,
            webhook = testWebhook,
            config = emptyList()
        )

        // Test installation
        testBot.install(testToken, installBody)

        // Test basic calculation
        val messageBody = MessageBotBody(
            message = "@calc 2 + 2",
            person = Person(id = "user123", name = "Test User")
        )

        val response = testBot.message(testToken, messageBody)
        assertTrue(response.success == true)
        assertNotNull(response.actions)
        assertTrue(response.actions?.isNotEmpty() == true)
        assertEquals("4", response.actions?.first()?.message)
    }

    @Test
    fun `test bot pause and resume`() {
        // Test pause
        testBot.pause(testToken)

        val messageBody = MessageBotBody(
            message = "@calc 3 * 4",
            person = Person(id = "user123", name = "Test User")
        )

        var response = testBot.message(testToken, messageBody)
        assertTrue(response.success == true) // Bot returns success=true when paused

        // Test resume
        testBot.resume(testToken)
        response = testBot.message(testToken, messageBody)
        assertTrue(response.success == true)
        assertEquals("12", response.actions?.first()?.message)
    }

    @Test
    fun `test calculator functionality`() {
        val testCases = listOf(
            "@calc 2 + 3 * 4" to "14",
            "@calc (2 + 3) * 4" to "20",
            "@calc sqrt(16) + 2" to "6",
            "@calc sin(0)" to "0",
            "@calc pi" to "3.141593",
            "@calc e" to "2.718282",
            "@calc log(100)" to "2",
            "@calc ln(e)" to "1"
        )

        for ((input, expected) in testCases) {
            val messageBody = MessageBotBody(
                message = input,
                person = Person(id = "user123", name = "Test User")
            )

            val response = testBot.message(testToken, messageBody)
            assertTrue(response.success == true)
            assertEquals(expected, response.actions?.first()?.message)
        }
    }

    @Test
    fun `test error handling`() {
        val testCases = listOf(
            "@calc 1/0" to "Error: Invalid expression: Division by zero",
            "@calc sqrt(-1)" to "Error: Invalid expression: Cannot calculate square root of negative number",
            "@calc log(-1)" to "Error: Invalid expression: Cannot calculate logarithm of non-positive number",
            "@calc 2 + * 3" to "Error: Invalid expression",
            "@calc" to "Usage: @calc [expression]\nExample: @calc 2 + 2"
        )

        for ((input, expected) in testCases) {
            val messageBody = MessageBotBody(
                message = input,
                person = Person(id = "user123", name = "Test User")
            )

            val response = testBot.message(testToken, messageBody)
            if (input == "@calc") {
                assertTrue(response.success == false)
                assertEquals(expected, response.actions?.first()?.message)
            } else {
                assertTrue(response.success == false)
                assertEquals(expected, response.actions?.first()?.message)
            }
        }
    }

    @Test
    fun `test invalid message handling`() {
        // Test null message
        var response = testBot.message(testToken, MessageBotBody(message = null))
        assertTrue(response.success == false)
        assertTrue(response.note?.contains("No message") == true)

        // Test non-calculator command
        response = testBot.message(testToken, MessageBotBody(message = "!other command"))
        assertTrue(response.success == true) // Non-calculator commands are ignored
    }
} 