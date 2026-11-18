package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestErrorMapper
import io.github.smyrgeorge.ktkit.ktor.httpclient.TypedRestClient
import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A [TypedRestClient] that fails with [RestClientErrorSpec]. By default it talks to a ktkit service:
 * [RestErrorMapper.default] reads every error body as an `ApiError`.
 *
 * @param json the codec of the default [client]; ignored when a [client] is given.
 */
open class RestClient(
    json: Json = Json,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(negotiation = { json(json) }),
    mapper: RestErrorMapper<RestClientErrorSpec, *> = RestErrorMapper.default(),
) : TypedRestClient<RestClientErrorSpec>(client = client, baseUrl = baseUrl, mapper = mapper)
