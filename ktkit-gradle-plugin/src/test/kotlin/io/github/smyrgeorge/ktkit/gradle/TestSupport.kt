package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Runs the configuration phase to completion, firing the project's `afterEvaluate` hooks. */
internal fun Project.evaluateNow() {
    (this as ProjectInternal).evaluate()
}

/** Asserts that evaluating [project] fails with [messagePart] somewhere in the cause chain. */
internal fun assertEvaluationFails(project: Project, messagePart: String) {
    val e = assertFailsWith<Exception> { project.evaluateNow() }
    val messages = generateSequence<Throwable>(e) { it.cause }.mapNotNull { it.message }.joinToString("\n")
    assertTrue(messagePart in messages, "expected '$messagePart' in:\n$messages")
}
