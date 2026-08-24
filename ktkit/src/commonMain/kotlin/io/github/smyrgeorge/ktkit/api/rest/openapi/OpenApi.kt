package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Documents a route for the OpenAPI specification. Place it directly above the route call:
 *
 * ```
 * @OpenApi(
 *     summary = "Returns a single user by id.",
 *     tags = ["users"],
 *     params = [
 *         OpenApiParam(name = "id", location = "path", type = "int", description = "The id of the user."),
 *         OpenApiParam(name = "verbose", type = "boolean", description = "Whether to include details."),
 *     ],
 *     responses = [OpenApiResponse(code = 404, description = "The user was not found.")],
 * )
 * GET("/{id}") { ... }
 * ```
 *
 * Expression annotations have SOURCE retention by language rules, so the compiler plugin collects
 * this annotation in its frontend (FIR) phase, where the arguments are already resolved and
 * constant-evaluated — references to `const` values, concatenations etc. are supported.
 *
 * NOTE: the parameter names are part of the contract with the compiler plugin
 * (`MetadataCollector`) — do not rename them without updating the plugin.
 *
 * @property summary The operation summary. Empty = absent.
 * @property description The operation description. Empty = absent.
 * @property tags The operation tags. Empty = the handler-derived default tag.
 * @property deprecated Non-empty marks the operation deprecated, with this text as the reason.
 * @property operationId Overrides the generated operationId. Empty = generated.
 * @property securityNone Suppresses the authentication error responses (401/403) of the operation.
 * @property params Documentation of the operation's parameters (see [OpenApiParam]).
 * @property responses Additional/overriding responses (see [OpenApiResponse]).
 * @property body The request body description. Empty = absent.
 * @property ignore Excludes the route from the specification.
 */
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApi(
    val summary: String = "",
    val description: String = "",
    val tags: Array<String> = [],
    val deprecated: String = "",
    val operationId: String = "",
    val securityNone: Boolean = false,
    val params: Array<OpenApiParam> = [],
    val responses: Array<OpenApiResponse> = [],
    val body: String = "",
    val ignore: Boolean = false,
)
