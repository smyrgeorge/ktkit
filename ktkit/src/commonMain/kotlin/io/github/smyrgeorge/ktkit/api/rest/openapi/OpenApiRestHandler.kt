package io.github.smyrgeorge.ktkit.api.rest.openapi

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.Application.Conf.OpenApi.Theme
import io.github.smyrgeorge.ktkit.Application.Conf.OpenApi.Ui.Scalar
import io.github.smyrgeorge.ktkit.Application.Conf.OpenApi.Ui.Swagger
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
 * - `GET <basePath>` — an interactive documentation page, either Swagger UI (default) or Scalar,
 *   selected via [Application.Conf.OpenApi.ui] ([Application.Conf.OpenApi.Ui.Swagger] /
 *   [Application.Conf.OpenApi.Ui.Scalar], each carrying the URLs of its own assets — loaded from
 *   a CDN by default; theme via [Application.Conf.OpenApi.theme]).
 * - `GET <basePath>/openapi.json` — the merged OpenAPI 3.1 document.
 *
 * The document is assembled lazily on first request (and cached) by merging the compile-time
 * fragments of all registered handlers (see [OpenApiDocBuilder]). Registered automatically by the
 * framework unless disabled via [Application.Conf.OpenApi.enabled].
 */
class OpenApiRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "${app.conf.openApi.basePath}$this"

    private val spec: String by lazy {
        OpenApiDocBuilder.build(app, Application.di.getAll<AbstractRestHandler>())
    }

    private val page: String by lazy {
        // The spec reference is kept relative to the page URL so the docs also work behind a
        // path-prefixing reverse proxy: '<base>/x/docs' resolves 'docs/openapi.json' correctly.
        val specUri = app.conf.openApi.basePath.substringAfterLast('/') + "/openapi.json"
        val conf = app.conf.openApi
        val title = app.name.escapeHtml()
        when (val ui = conf.ui) {
            is Swagger -> swagger(ui, conf.theme, title, specUri)
            is Scalar -> scalar(ui, conf.theme, title, specUri)
        }
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
    private fun themeStyle(theme: Theme): String {
        val dark = """
            html { background: #ffffff; filter: invert(88%) hue-rotate(180deg); }
            img, video, iframe { filter: invert(100%) hue-rotate(180deg); }
        """.trimIndent()
        return when (theme) {
            Theme.LIGHT -> ""
            Theme.DARK -> "<style>\n$dark\n</style>"
            Theme.AUTO -> "<style>\n@media (prefers-color-scheme: dark) {\n$dark\n}\n</style>"
        }
    }

    @Suppress("JSUnresolvedReference")
    private fun swagger(ui: Swagger, theme: Theme, title: String, specUri: String): String =
        //language=html
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>$title - API Docs</title>
          <link rel="stylesheet" href="${ui.css.escapeHtml()}"/>
          ${themeStyle(theme)}
        </head>
        <body>
        <div id="swagger-ui"></div>
        <script src="${ui.js.escapeHtml()}" crossorigin></script>
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

    /** Scalar ships its own dark theme, so the theme maps directly to its configuration. */
    private fun scalarThemeOptions(theme: Theme): String = when (theme) {
        Theme.AUTO -> "darkMode: window.matchMedia('(prefers-color-scheme: dark)').matches,"
        Theme.LIGHT -> "forceDarkModeState: 'light', hideDarkModeToggle: true,"
        Theme.DARK -> "forceDarkModeState: 'dark', hideDarkModeToggle: true,"
    }

    @Suppress("JSUnresolvedReference")
    private fun scalar(ui: Scalar, theme: Theme, title: String, specUri: String): String =
        //language=html
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>$title - API Docs</title>
        </head>
        <body>
        <div id="scalar-api-reference"></div>
        <script src="${ui.js.escapeHtml()}" crossorigin></script>
        <script>
          Scalar.createApiReference('#scalar-api-reference', {
            url: '$specUri',
            ${scalarThemeOptions(theme)}
          });
        </script>
        </body>
        </html>
        """.trimIndent()
}
