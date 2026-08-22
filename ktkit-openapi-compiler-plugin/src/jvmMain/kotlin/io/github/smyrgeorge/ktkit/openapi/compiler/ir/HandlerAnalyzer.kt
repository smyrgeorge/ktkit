package io.github.smyrgeorge.ktkit.openapi.compiler.ir

import io.github.smyrgeorge.ktkit.openapi.compiler.ir.route.RouteAnalyzer
import io.github.smyrgeorge.ktkit.openapi.compiler.ir.route.UriParser
import io.github.smyrgeorge.ktkit.openapi.compiler.ir.schema.JsonNode
import io.github.smyrgeorge.ktkit.openapi.compiler.ir.schema.JsonNode.Companion.obj
import io.github.smyrgeorge.ktkit.openapi.compiler.ir.schema.JsonNode.Companion.str
import io.github.smyrgeorge.ktkit.openapi.compiler.ir.schema.SchemaGenerator
import io.github.smyrgeorge.ktkit.openapi.compiler.utils.KtkitNames
import io.github.smyrgeorge.ktkit.openapi.compiler.utils.MetadataStore
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class HandlerAnalyzer(
    private val messageCollector: MessageCollector,
    private val store: MetadataStore,
) {

    /** Returns the handler's OpenAPI fragment as a JSON string, or `null` when there is nothing to document. */
    fun analyze(irClass: IrClass, file: IrFile): String? {
        val classFq = irClass.fqNameWhenAvailable?.asString() ?: return null
        val chain = listOf(irClass) + irClass.allSuperClasses()

        // Anonymous handlers never require authentication — drives the 401/403 responses.
        // NOTE: security schemes/requirements are intentionally not emitted (yet).
        val anonymous = chain.any { it.fqNameWhenAvailable == KtkitNames.ANONYMOUS_REST_HANDLER }

        val uriFn = chain.findDeclaredFunction {
            it.name.asString() == KtkitNames.URI && it.extensionReceiverParam() != null && it.body != null
        }
        val template = uriFn?.let { UriParser.parse(it) }
        if (template == null) {
            messageCollector.reportWarning(
                file = file,
                offset = irClass.startOffset,
                message = "could not statically evaluate $classFq.uri(); using the per-route paths as-is."
            )
        }

        val routesFn = chain.findDeclaredFunction { it.name.asString() == KtkitNames.ROUTES && it.body != null }
        if (routesFn == null) {
            messageCollector.reportWarning(
                file = file,
                offset = irClass.startOffset,
                message = "could not find the routes() implementation of $classFq; skipped."
            )
            return null
        }

        val routeCalls = collectRouteCalls(routesFn, file, irClass)
        if (routeCalls.isEmpty()) return null

        val routesFile = routesFn.fileOrNull ?: file
        val schemas = SchemaGenerator { message ->
            messageCollector.reportWarning(file, irClass.startOffset, "$classFq: $message")
        }
        val routeAnalyzer = RouteAnalyzer(
            messageCollector = messageCollector,
            store = store,
            irClass = irClass,
            file = routesFile,
            template = template,
            anonymous = anonymous,
            schemas = schemas,
        )

        val paths = LinkedHashMap<String, JsonNode.Obj>()
        routeCalls.forEach { (groupPrefix, call) ->
            val route = routeAnalyzer.analyze(groupPrefix, call) ?: return@forEach
            val pathItem = paths.getOrPut(route.path) { JsonNode.Obj() }
            if (pathItem[route.verb] != null) {
                messageCollector.reportWarning(
                    file = routesFile,
                    offset = call.startOffset,
                    message = "duplicate route '${route.verb.uppercase()} ${route.path}' in $classFq; keeping the first one."
                )
            } else {
                pathItem[route.verb] = route.operation
            }
        }
        if (paths.isEmpty()) return null

        return fragment(classFq, paths, schemas)
    }

    /**
     * Collects the verb calls of a `routes()` body, together with the path prefix of any enclosing
     * Ktor `route("...") { }` groups (Ktor prepends the group segments at runtime).
     */
    private fun collectRouteCalls(
        routesFn: IrSimpleFunction,
        file: IrFile,
        irClass: IrClass,
    ): List<Pair<String, IrCall>> {
        val routeCalls = mutableListOf<Pair<String, IrCall>>()
        routesFn.body!!.acceptVoid(object : IrVisitorVoid() {
            private var groupPrefix = ""

            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitCall(expression: IrCall) {
                if (expression.calleeName() == "route" &&
                    expression.resolvedCallee().fqNameWhenAvailable?.asString() == "io.ktor.server.routing.route"
                ) {
                    val segment = expression.regularArgument("path")?.constString()
                    if (segment == null) {
                        messageCollector.reportWarning(
                            file = file,
                            offset = expression.startOffset,
                            message = "non-constant route(...) group in ${irClass.name}; its routes are not documented."
                        )
                        return // skip the whole subtree — its paths cannot be resolved
                    }
                    val previous = groupPrefix
                    val trimmed = segment.trim('/')
                    if (trimmed.isNotEmpty()) groupPrefix = "$previous/$trimmed"
                    expression.acceptChildrenVoid(this)
                    groupPrefix = previous
                    return
                }
                expression.acceptChildrenVoid(this)
                if (expression.calleeName() in KtkitNames.VERBS &&
                    expression.calleeParentClassFq() == KtkitNames.ABSTRACT_REST_HANDLER
                ) routeCalls += groupPrefix to expression
            }
        })
        return routeCalls
    }

    private fun fragment(classFq: String, paths: Map<String, JsonNode.Obj>, schemas: SchemaGenerator): String {
        val fragment = obj("x-handler" to str(classFq))
        val pathsObj = JsonNode.Obj()
        paths.forEach { (path, item) -> pathsObj[path] = item }
        fragment["paths"] = pathsObj

        val components = JsonNode.Obj()
        if (schemas.components.isNotEmpty()) {
            val schemasObj = JsonNode.Obj()
            schemas.components.forEach { (key, schema) -> schemasObj[key] = schema }
            components["schemas"] = schemasObj
        }
        if (components.isNotEmpty()) fragment["components"] = components
        return fragment.renderToString()
    }
}
