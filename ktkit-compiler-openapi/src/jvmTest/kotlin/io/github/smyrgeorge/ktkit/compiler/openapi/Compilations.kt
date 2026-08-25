package io.github.smyrgeorge.ktkit.compiler.openapi

/**
 * The shared compilations of the integration tests — one per scenario family, compiled once (per
 * test JVM) and asserted on by the test classes of the packages under test.
 */
internal object Compilations {

    /** Happy-path handlers: routes, parameters, metadata annotations, response shapes. */
    val handlers: CompilerTestSupport.Compilation by lazy {
        //language=kotlin
        CompilerTestSupport.compile(
            "handlers.kt" to $$"""
                package analysis

                import arrow.core.Either
                import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
                import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
                import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
                import io.github.smyrgeorge.ktkit.api.rest.impl.AnonymousRestHandler
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApi
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiIgnore
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiInfo
                import io.ktor.http.HttpStatusCode
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.route
                import kotlinx.coroutines.flow.Flow
                import kotlinx.serialization.Serializable

                @Serializable
                data class ItemDto(val id: Int, val name: String? = null)

                enum class Color { RED, GREEN }

                const val SUMMARY = "From a const."

                class ItemsRestHandler : AbstractRestHandler() {
                    override fun String.uri(): String = "/api/v1/items$this"

                    override fun Route.routes() {
                        @OpenApi(
                            summary = SUMMARY,
                            description = "Lists items.",
                            tags = ["items", "catalog"],
                        )
                        GET<List<ItemDto>>("") {
                            @OpenApiInfo("Max items returned.")
                            val limit = queryParam("limit").asIntOrNull()
                            val trace = header("x-trace").asStringOrNull()
                            val colors = queryParams("color")
                            val verbose = @OpenApiInfo("Verbose output.") queryParam("verbose").asBooleanOrNull()
                            TODO()
                        }

                        GET<ItemDto>("/{id}") {
                            val id = pathVariable("id").asLong()
                            TODO()
                        }

                        POST<ItemDto>("/create") {
                            val item = body<ItemDto>()
                            TODO()
                        }

                        @OpenApi(deprecated = "Use POST /create instead.")
                        PUT<Unit>("/legacy", onSuccessHttpStatusCode = HttpStatusCode.Accepted) {
                            TODO()
                        }

                        DELETE<Unit>("/custom-status", onSuccessHttpStatusCode = HttpStatusCode(299, "Custom")) {
                            TODO()
                        }

                        GET<Either<ErrorSpec, ItemDto>>("/either") {
                            NotFound("nope")
                            TODO()
                        }

                        GET<Flow<ItemDto>>("/stream") { TODO() }

                        GET<String>("/enum") {
                            val color = queryParam("color").asEnum<Color>()
                            TODO()
                        }

                        @OpenApiIgnore
                        GET<String>("/internal") { TODO() }

                        route("grouped") {
                            GET<String>("/inside") { TODO() }
                        }

                        GET<String>("/opt/{id?}") {
                            val id = pathVariable("id").asStringOrNull()
                            TODO()
                        }
                    }
                }

                class OpenRestHandler : AnonymousRestHandler() {
                    override fun String.uri(): String = "/open$this"
                    override fun Route.routes() {
                        GET<String>("/ping") { TODO() }
                    }
                }
            """.trimIndent()
        )
    }

    /** `@Serializable` types of every schema-relevant shape, referenced from routes. */
    val schemas: CompilerTestSupport.Compilation by lazy {
        //language=kotlin
        CompilerTestSupport.compile(
            "p2.kt" to """
                package p2
                import kotlinx.serialization.Serializable
                @Serializable
                data class Data(val a: Int)
            """.trimIndent(),
            "p3.kt" to """
                package p3
                import kotlinx.serialization.Serializable
                @Serializable
                data class Data(val b: String)
            """.trimIndent(),
            "schemas.kt" to $$"""
                package schemagen

                import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiInfo
                import io.ktor.server.routing.Route
                import kotlin.time.Instant
                import kotlin.uuid.Uuid
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.SerialName
                import kotlinx.serialization.Serializable
                import kotlinx.serialization.Transient
                import kotlinx.serialization.descriptors.SerialDescriptor
                import kotlinx.serialization.encoding.Decoder
                import kotlinx.serialization.encoding.Encoder

                @OpenApiInfo("An item of the system.")
                @Serializable
                data class ItemDto(
                    @OpenApiInfo("The id.")
                    val id: Int,
                )

                @Serializable
                data class PropsDto(
                    val req: String,
                    @SerialName("renamed") val original: Int,
                    @Transient val hidden: Boolean = false,
                    val defaulted: Int = 5,
                    val id: Uuid,
                    val at: Instant,
                )

                enum class Status { ACTIVE, INACTIVE }

                @Serializable
                data class NullableDto(
                    val opt: String? = null,
                    val maybe: Status? = null,
                    val status: Status = Status.ACTIVE,
                )

                @Serializable
                open class BaseDto(val common: String)

                @Serializable
                class ChildDto(val own: Int = 1) : BaseDto("x")

                @Serializable
                sealed interface Shape

                @Serializable
                @SerialName("circle")
                data class Circle(val radius: Double) : Shape

                @Serializable
                data class Square(val side: Double) : Shape

                @Serializable
                data class Page<T>(val items: List<T>, val total: Long)

                @JvmInline
                value class UserId(val value: Long)

                @Serializable
                data class WithValueClass(val userId: UserId)

                @Serializable
                data class Node(val children: List<Node> = emptyList())

                @Serializable
                object SingletonDto

                class Plain(val x: Int)

                object CustomSer : KSerializer<Custom> {
                    override val descriptor: SerialDescriptor get() = TODO()
                    override fun serialize(encoder: Encoder, value: Custom): Unit = TODO()
                    override fun deserialize(decoder: Decoder): Custom = TODO()
                }

                @Serializable(with = CustomSer::class)
                class Custom(val x: Int)

                class SchemaRestHandler : AbstractRestHandler() {
                    override fun String.uri(): String = "/s$this"

                    override fun Route.routes() {
                        GET<ItemDto>("/item") { TODO() }
                        GET<PropsDto>("/props") { TODO() }
                        GET<NullableDto>("/nullable") { TODO() }
                        GET<ChildDto>("/child") { TODO() }
                        GET<Shape>("/shape") { TODO() }
                        GET<Page<ItemDto>>("/page") { TODO() }
                        GET<WithValueClass>("/value") { TODO() }
                        GET<Node>("/node") { TODO() }
                        GET<Map<String, ItemDto>>("/map") { TODO() }
                        GET<SingletonDto>("/singleton") { TODO() }
                        GET<Custom>("/custom") { TODO() }
                        GET<Plain>("/plain") { TODO() }
                        GET<p2.Data>("/d2") { TODO() }
                        GET<p3.Data>("/d3") { TODO() }
                    }
                }
            """.trimIndent()
        )
    }

    /**
     * Analysis-limit scenarios: routes and handlers the plugin must skip with a warning rather
     * than document wrongly, plus the opt-outs (`@OpenApiIgnore` on a class, a hand-written
     * override).
     */
    val edgeCases: CompilerTestSupport.Compilation by lazy {
        CompilerTestSupport.compile(
            //language=kotlin
            "warnings.kt" to $$"""
                package warnings

                import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApi
                import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiIgnore
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.route

                private fun dynamic(): String = "/d"

                class DynamicPathHandler : AbstractRestHandler() {
                    override fun String.uri(): String = this
                    override fun Route.routes() {
                        GET<String>(dynamic()) { TODO() }
                    }
                }

                class DynamicUriHandler : AbstractRestHandler() {
                    private val prefix: String = dynamic()
                    override fun String.uri(): String = prefix + this
                    override fun Route.routes() {
                        GET<String>("/as-is") { TODO() }
                    }
                }

                class OrphanParamHandler : AbstractRestHandler() {
                    override fun String.uri(): String = "/o$this"
                    override fun Route.routes() {
                        GET<String>("/fixed") {
                            val id = pathVariable("id").asInt()
                            TODO()
                        }
                    }
                }

                class DuplicateRouteHandler : AbstractRestHandler() {
                    override fun String.uri(): String = this
                    override fun Route.routes() {
                        @OpenApi(summary = "first")
                        GET<String>("/dup") { TODO() }
                        @OpenApi(summary = "second")
                        GET<String>("/dup") { TODO() }
                    }
                }

                class DynamicGroupHandler : AbstractRestHandler() {
                    override fun String.uri(): String = "/g$this"
                    override fun Route.routes() {
                        route(dynamic()) {
                            GET<String>("/hidden") { TODO() }
                        }
                        GET<String>("/ok") { TODO() }
                    }
                }

                @OpenApiIgnore
                class IgnoredHandler : AbstractRestHandler() {
                    override fun String.uri(): String = this
                    override fun Route.routes() {
                        GET<String>("/never") { TODO() }
                    }
                }

                class HandWrittenHandler : AbstractRestHandler() {
                    override fun String.uri(): String = this
                    override fun Route.routes() {
                        GET<String>("/hand") { TODO() }
                    }
                    override fun openApiSpec(): String = "{\"hand\":\"written\"}"
                }

                abstract class AbstractBase : AbstractRestHandler() {
                    override fun String.uri(): String = "/base$this"
                    override fun Route.routes() {
                        GET<String>("/route") { TODO() }
                    }
                }

                class ConcreteOfAbstract : AbstractBase()

                class PlusUriHandler : AbstractRestHandler() {
                    override fun String.uri(): String = "/plus" + "/chain" + this
                    override fun Route.routes() {
                        GET<String>("/x") { TODO() }
                    }
                }
            """.trimIndent()
        )
    }
}
