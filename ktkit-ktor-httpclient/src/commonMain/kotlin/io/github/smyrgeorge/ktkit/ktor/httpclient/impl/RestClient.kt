package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json

class RestClient(
    json: Json,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(json = json),
    mapError: suspend context(Raise<RestClientErrorSpec>) HttpResponse.() -> RestClientErrorSpec,
) : AbstractRestClient(baseUrl = baseUrl, json = json, client = client, mapError = mapError)
