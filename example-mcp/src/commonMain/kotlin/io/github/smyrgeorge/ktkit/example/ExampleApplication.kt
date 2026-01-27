package io.github.smyrgeorge.ktkit.example

import io.github.smyrgeorge.ktkit.Application
import io.ktor.http.HttpMethod
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class UserInfo(
    val id: Int,
    val name: String,
    val email: String,
    val active: Boolean
)

class ExampleApplication

fun configureServer(): Server {
    val server = Server(
        Implementation(
            name = "mcp-kotlin test server",
            version = "0.1.0",
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    server.addPrompt(
        name = "Kotlin Developer",
        description = "Develop small kotlin applications",
        arguments = listOf(
            PromptArgument(
                name = "Project Name",
                description = "Project name for the new project",
                required = true,
            ),
        ),
    ) { request ->
        GetPromptResult(
            messages = listOf(
                PromptMessage(
                    role = Role.User,
                    content = TextContent(
                        "Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>",
                    ),
                ),
            ),
            description = "Description for ${request.name}",
        )
    }

    // Add a tool
    server.addTool(
        name = "kotlin-sdk-tool",
        description = "A test tool",
    ) { _ ->
        CallToolResult(
            content = listOf(TextContent("Hello, world!")),
        )
    }

    // Add a tool that returns JSON from a data class
    server.addTool(
        name = "get-user-info",
        description = "Returns user information as JSON",
    ) { _ ->
        val userInfo = UserInfo(
            id = 1,
            name = "John Doe",
            email = "john.doe@example.com",
            active = true
        )
        val json = Json.encodeToString(userInfo)
        CallToolResult(
            content = emptyList(),
            structuredContent = Json.encodeToJsonElement(userInfo).jsonObject
        )
    }

    // Add a resource
    server.addResource(
        uri = "https://search.com/",
        name = "Web Search",
        description = "Web search engine",
        mimeType = "text/html",
    ) { request ->
        ReadResourceResult(
            contents = listOf(
                TextResourceContents("Placeholder content for ${request.uri}", request.uri, "text/html"),
            ),
        )
    }

    return server
}

fun start() {
    Application(
        name = Application::class.simpleName!!,
        conf = Application.Conf(
            host = "localhost",
            port = 8080,
        ),
        configure = {
            ktor {
                install(CORS) {
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Delete)
                    allowNonSimpleContentTypes = true
                    anyHost() // TODO: Don't do this in production if possible. Try to limit it.
                }
                mcp {
                    configureServer()
                }
            }
        },
    ).start()
}
