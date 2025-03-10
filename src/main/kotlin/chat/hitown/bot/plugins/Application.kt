/**
 * The Ktor server.
 */

package chat.hitown.bot.plugins

import chat.hitown.bot.bot
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import org.slf4j.event.Level
import kotlinx.serialization.json.Json
import java.util.*

fun main() {
    embeddedServer(
        factory = Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0",
        module = {
            configureRouting()
        }
    ).start(wait = true)
}

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.UserAgent)
        allowCredentials = true
        maxAgeInSeconds = 3600
        anyHost()
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            "$method $path - $status - $duration ms"
        }
    }

    routing {
        get("/") {
            call.respond(bot.details)
        }

        get("/health") {
            call.respond(mapOf(
                "status" to "healthy",
                "version" to "1.0.0",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        get("/metrics") {
            call.respond(mapOf(
                "uptime" to System.currentTimeMillis(),
                "groups" to bot.groupCount,
                "messages_processed" to bot.messagesProcessed,
                "errors" to bot.errorCount
            ))
        }

        post("/install") {
            val body = call.receive<InstallBotBody>()
            if (!bot.validateInstall(body.secret)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val token = UUID.randomUUID().toString()
            bot.install(token, body)
            call.respond(InstallBotResponse(token = token))
        }

        post("/reinstall") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val body = call.receive<ReinstallBotBody>()
            bot.reinstall(token, body.config)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/uninstall") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            bot.uninstall(token)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/message") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val body = call.receive<MessageBotBody>()
            val response = bot.message(token, body)
            call.respond(response)
        }

        post("/pause") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            bot.pause(token)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/resume") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            bot.resume(token)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
