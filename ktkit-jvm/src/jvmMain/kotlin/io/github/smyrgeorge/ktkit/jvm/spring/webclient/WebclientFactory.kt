package io.github.smyrgeorge.ktkit.jvm.spring.webclient

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.util.MimeType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.builder
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import tools.jackson.databind.json.JsonMapper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object WebclientFactory {
    /**
     * Creates a configured instance of a [WebClient] with specified connection settings, timeouts,
     * and codecs for JSON processing.
     *
     * @param om the JSON mapper used for encoding and decoding JSON data.
     * @param connectionTimeout the timeout duration for establishing connections, default is 2 seconds.
     * @param readTimeout the timeout duration for reading data, default is 10 seconds.
     * @param maxConnections the maximum number of connections allowed in the connection pool, default is 128.
     * @param pendingAcquireMaxCount the maximum number of pending connection requests per endpoint, default is 1024.
     * @param poolMaxIdleTime the maximum idle time for connections in the pool, default is 1 minute.
     * @param poolEvictInBackground the interval at which idle connections are evicted from the pool, default is 120 seconds.
     * @param codecMemoryMaxSize the maximum in-memory size for JSON decoding, default is 16 MB.
     * @param codecMimeTypes an array of supported MIME types for JSON processing, default is APPLICATION_JSON.
     * @return a configured instance of [WebClient].
     */
    fun create(
        om: JsonMapper,
        connectionTimeout: Duration = 2.seconds,
        readTimeout: Duration = 10.seconds,
        maxConnections: Int = 128,
        pendingAcquireMaxCount: Int = 1024,
        poolMaxIdleTime: Duration = 1.minutes,
        poolEvictInBackground: Duration = 120.seconds,
        codecMemoryMaxSize: Int = 16 * 1024 * 1024,
        codecMimeTypes: Array<MimeType> = arrayOf(APPLICATION_JSON)
    ): WebClient {
        val httpClient = HttpClient
            .create(
                ConnectionProvider
                    .builder("WebclientFactoryConnectionProvider")
                    .maxConnections(maxConnections)
                    .pendingAcquireMaxCount(pendingAcquireMaxCount)
                    .maxIdleTime(poolMaxIdleTime.toJavaDuration())
                    .evictInBackground(poolEvictInBackground.toJavaDuration())
                    .build()
            )
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                connectionTimeout.inWholeMilliseconds.toInt()
            )
            .doOnConnected { it.addHandlerFirst(ReadTimeoutHandler(readTimeout.inWholeSeconds.toInt())) }

        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer ->
                configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(om, *codecMimeTypes))
                configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(om, *codecMimeTypes))
                configurer.defaultCodecs().maxInMemorySize(codecMemoryMaxSize)
            }.build()

        return builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .build()
    }
}
