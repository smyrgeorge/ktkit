package io.github.smyrgeorge.ktkit.api.rest.openapi

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.api.rest.impl.AnonymousRestHandler
import io.github.smyrgeorge.ktkit.util.getAll
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Serves the OpenAPI documentation of the application, mounted on
 * [Application.Conf.OpenApi.basePath] (`/api/docs` by default):
 * - `GET <basePath>` — an interactive Swagger UI page (assets loaded from the unpkg CDN by default,
 *   overridable via [Application.Conf.OpenApi.swaggerUiCss] / [Application.Conf.OpenApi.swaggerUiJs];
 *   theme via [Application.Conf.OpenApi.theme]).
 * - `GET <basePath>/openapi.json` — the merged OpenAPI 3.1 document.
 *
 * The document is assembled lazily on first request (and cached) by merging the compile-time
 * fragments of all registered handlers (see [OpenApiDocBuilder]). Registered automatically by the
 * framework unless disabled via [Application.Conf.OpenApi.enabled].
 *
 * The routes are mounted as plain Ktor routes (no authentication, non-JSON content types) and are
 * excluded from the specification itself.
 */
@OpenApiIgnore
class OpenApiRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "${app.conf.openApi.basePath}$this"

    private val spec: String by lazy {
        OpenApiDocBuilder.build(app, Application.di.getAll<AbstractRestHandler>())
    }

    private val page: String by lazy {
        // The spec reference is kept relative to the page URL so the docs also work behind a
        // path-prefixing reverse proxy: '<base>/x/docs' resolves 'docs/openapi.json' correctly.
        val specUri = app.conf.openApi.basePath.substringAfterLast('/') + "/openapi.json"
        swaggerUi(conf = app.conf.openApi, title = app.name.escapeHtml(), specUri = specUri)
    }

    override fun Route.routes() {
        get("".uri()) {
            call.respondText(page, ContentType.Text.Html)
        }
        get("/openapi.json".uri()) {
            call.respondText(spec, ContentType.Application.Json)
        }
    }

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    /**
     * Swagger UI ships no dark theme, so the dark look is a whole-page `invert + hue-rotate`
     * filter (hue rotation keeps the method colors recognizable) with media re-inverted.
     */
    private fun themeStyle(theme: Application.Conf.OpenApi.Theme): String {
        val dark = """
            html { background: #ffffff; filter: invert(88%) hue-rotate(180deg); }
            img, video, iframe { filter: invert(100%) hue-rotate(180deg); }
        """.trimIndent()
        return when (theme) {
            Application.Conf.OpenApi.Theme.LIGHT -> ""
            Application.Conf.OpenApi.Theme.DARK -> "<style>\n$dark\n</style>"
            Application.Conf.OpenApi.Theme.AUTO -> "<style>\n@media (prefers-color-scheme: dark) {\n$dark\n}\n</style>"
        }
    }

    @Suppress("JSUnresolvedReference")
    //language=html
    private fun swaggerUi(conf: Application.Conf.OpenApi, title: String, specUri: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>$title - API Docs</title>
          <link rel="stylesheet" href="${conf.swaggerUiCss.escapeHtml()}"/>
          ${themeStyle(conf.theme)}
        </head>
        <body>
        <div id="swagger-ui"></div>
        <script src="${conf.swaggerUiJs.escapeHtml()}" crossorigin></script>
        <script>
          window.onload = () => {
            window.ui = SwaggerUIBundle({
              url: '$specUri',
              dom_id: '#swagger-ui',
            });
          };
        </script>
        </body>
        </html>
    """.trimIndent()
}
