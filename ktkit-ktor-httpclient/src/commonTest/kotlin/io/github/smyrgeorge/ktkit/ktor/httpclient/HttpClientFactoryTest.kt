package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import kotlinx.coroutines.job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.assertNull

class HttpClientFactoryTest {
    @Test
    fun createInstallsNegotiationTimeoutsAndTheDefaultRequest() {
        HttpClientFactory.create(json = testJson).use { client ->
            assertNotNull(client.pluginOrNull(ContentNegotiation))
            assertNotNull(client.pluginOrNull(HttpTimeout))
            assertNotNull(client.pluginOrNull(DefaultRequest))
        }
    }

    /** Logging is opt-in: it is off unless `enableLogging` is passed. */
    @Test
    fun loggingIsNotInstalledByDefault() {
        HttpClientFactory.create(json = testJson).use { client ->
            assertNull(client.pluginOrNull(Logging))
        }
    }

    @Test
    fun loggingIsInstalledWhenEnabled() {
        HttpClientFactory.create(json = testJson, enableLogging = true, logLevel = LogLevel.BODY).use { client ->
            assertNotNull(client.pluginOrNull(Logging))
        }
    }

    @Test
    fun maxConnectionsReachesTheEngineConfiguration() {
        HttpClientFactory.create(json = testJson).use { client ->
            assertEquals(128, assertIs<CIOEngineConfig>(client.engine.config).maxConnectionsCount)
        }
        HttpClientFactory.create(json = testJson, maxConnections = 7).use { client ->
            assertEquals(7, assertIs<CIOEngineConfig>(client.engine.config).maxConnectionsCount)
        }
    }

    /**
     * Every call hands back its own client with its own lifecycle: closing one must not take the
     * other down with it. Callers own what they are given, so a shared or cached client would be a
     * real bug — `assertNotSame` alone would not catch it, hence the liveness check.
     */
    @Test
    fun eachCallReturnsAnIndependentClient() {
        val a = HttpClientFactory.create(json = testJson)
        val b = HttpClientFactory.create(json = testJson)
        assertNotSame(a, b)
        a.close()
        assertFalse(a.coroutineContext.job.isActive)
        assertTrue(b.coroutineContext.job.isActive)
        b.close()
    }
}
