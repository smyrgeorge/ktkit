package io.github.smyrgeorge.ktkit.compiler.openapi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals

/**
 * Compiles Kotlin sources in-process with the ktkit OpenAPI compiler plugin attached (loaded via
 * `-Xplugin` from this module's own build output) and against the test runtime classpath (the
 * ktkit runtime, Ktor, Arrow, ...). The generated fragments are read back by loading the compiled
 * handler class and invoking its synthesized `openApiSpec()` override — the same call the ktkit
 * runtime makes.
 */
object CompilerTestSupport {

    class Compilation(
        val exitCode: ExitCode,
        val messages: List<String>,
        private val outDir: File,
    ) {
        /** The warnings reported by the plugin (without the "ktkit-openapi: " prefix). */
        val warnings: List<String> =
            messages.filter { "ktkit-openapi: " in it }.map { it.substringAfter("ktkit-openapi: ") }

        private val classLoader: ClassLoader by lazy {
            URLClassLoader(arrayOf(outDir.toURI().toURL()), CompilerTestSupport::class.java.classLoader)
        }

        fun assertOk() {
            assertEquals(ExitCode.OK, exitCode, "compilation failed:\n" + messages.joinToString("\n"))
        }

        /** Instantiates the compiled handler and returns what its `openApiSpec()` returns. */
        fun openApiSpec(classFq: String): String? {
            val cls = classLoader.loadClass(classFq)
            val instance = cls.getDeclaredConstructor().newInstance()
            return cls.getMethod("openApiSpec").invoke(instance) as String?
        }

        /** The handler's fragment parsed as JSON (fails the test on a null fragment). */
        fun fragment(classFq: String): JsonObject {
            val spec = openApiSpec(classFq)
                ?: throw AssertionError("no OpenAPI fragment was generated for $classFq")
            return Json.parseToJsonElement(spec).jsonObject
        }
    }

    fun compile(vararg sources: Pair<String, String>): Compilation {
        val root = createTempDirectory("ktkit-openapi-test").toFile().apply { deleteOnExit() }
        val srcDir = File(root, "src").apply { mkdirs() }
        val outDir = File(root, "out").apply { mkdirs() }
        val files = sources.map { (name, content) -> File(srcDir, name).apply { writeText(content) } }

        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val collector = object : MessageCollector {
            override fun clear() = messages.clear()
            override fun hasErrors(): Boolean = messages.any { it.first.isError }
            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?,
            ) {
                messages += severity to message
            }
        }

        val arguments = K2JVMCompilerArguments().apply {
            freeArgs = files.map { it.absolutePath }
            destination = outDir.absolutePath
            classpath = System.getProperty("java.class.path")
            pluginClasspaths = pluginClasspaths()
            jvmTarget = "21"
            noStdlib = true
            noReflect = true
        }
        val exitCode = K2JVMCompiler().exec(collector, Services.EMPTY, arguments)
        return Compilation(exitCode, messages.map { (severity, message) -> "$severity: $message" }, outDir)
    }

    /**
     * The plugin's own build output: the classes directory (from the registrar's code source) and
     * the processed-resources directory carrying the `META-INF/services` registration the
     * compiler's plugin loader discovers the registrar through.
     */
    private fun pluginClasspaths(): Array<String> {
        val registrar = OpenApiCompilerPluginRegistrar::class.java
        val classes = File(registrar.protectionDomain.codeSource.location.toURI())
        if (classes.extension == "jar") return arrayOf(classes.absolutePath)
        val services = registrar.classLoader
            .getResource("META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar")
            ?: error("the plugin's META-INF/services registration is not on the test classpath")
        val resourcesRoot = File(services.toURI()).parentFile.parentFile.parentFile
        return arrayOf(classes.absolutePath, resourcesRoot.absolutePath).distinct().toTypedArray()
    }
}
