// HttpMethod.kt
/**
 * Represents HTTP methods.
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

// Header.kt
/**
 * Represents a single HTTP header.
 * @property name The name of the header.
 * @property value The value of the header.
 */
data class Header(val name: String, val value: String)

// HttpRequest.kt
/**
 * Represents an HTTP request.
 * This is an immutable data class representing the configured request.
 *
 * @property uri The Uniform Resource Identifier for the request.
 * @property method The HTTP method (GET, POST, etc.).
 * @property body The optional request body, typically used with POST, PUT, PATCH.
 * @property headers A list of HTTP headers for the request.
 */
data class HttpRequest(
    val uri: String,
    val method: HttpMethod,
    val body: String? = null,
    val headers: List<Header> = emptyList()
)

// HeadersBuilder.kt
/**
 * A DSL builder for constructing a list of HTTP headers.
 * This builder is used within the `httpRequest` DSL.
 */
@DslMarker
annotation class HttpDslMarker

@HttpDslMarker
class HeadersBuilder {
    // Internal mutable list to accumulate headers
    internal val headersList = mutableListOf<Header>()

    /**
     * Adds a header with the given name and value.
     * Example: `header("Content-Type", "application/json")`
     */
    fun header(name: String, value: String) {
        headersList.add(Header(name, value))
    }

    /**
     * Infix function to add a header using a "Name" to "Value" syntax.
     * Example: `"Authorization" to "Bearer token"`
     */
    infix fun String.to(value: String) {
        headersList.add(Header(this, value))
    }

    /**
     * Allows adding a Pair directly as a header.
     * Example: `+"X-Custom-Header" to "SomeValue"` (though `to` is more idiomatic)
     * or `add("X-Custom-Header" to "SomeValue")`
     */
    fun add(header: Pair<String, String>) {
        headersList.add(Header(header.first, header.second))
    }
}

// HttpRequestBuilder.kt
/**
 * A DSL builder for constructing an [HttpRequest].
 * This is the main entry point for the DSL.
 */
@HttpDslMarker
class HttpRequestBuilder {
    /** The URI for the HTTP request. Must be set. */
    var uri: String = ""

    /** The HTTP method. Defaults to GET. */
    var method: HttpMethod = HttpMethod.GET

    /** The optional request body. */
    var body: String? = null

    // Internal list to store headers collected by the HeadersBuilder
    private val collectedHeaders = mutableListOf<Header>()

    /**
     * DSL function to configure headers for the request.
     * Takes a lambda with [HeadersBuilder] as its receiver.
     * Example:
     * ```
     * headers {
     * "Content-Type" to "application/json"
     * header("Accept", "application/xml")
     * }
     * ```
     */
    fun headers(block: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder()
        builder.block() // Execute the user's header definitions
        this.collectedHeaders.addAll(builder.headersList)
    }

    /**
     * Builds and returns an immutable [HttpRequest] object.
     * @throws IllegalStateException if the URI is not set.
     */
    fun build(): HttpRequest {
        if (uri.isBlank()) {
            throw IllegalStateException("URI must be set for an HTTP request.")
        }
        return HttpRequest(
            uri = uri,
            method = method,
            body = body,
            headers = collectedHeaders.toList() // Create an immutable list
        )
    }
}

// Dsl.kt (Top-level function)
/**
 * Top-level DSL function to create an [HttpRequest].
 *
 * @param block A lambda expression with [HttpRequestBuilder] as its receiver,
 * allowing declarative configuration of the HTTP request.
 * @return An immutable [HttpRequest] object.
 *
 * Example Usage:
 * ```
 * val request = httpRequest {
 * uri = "[https://api.example.com/users](https://api.example.com/users)"
 * method = HttpMethod.POST
 * headers {
 * "Content-Type" to "application/json"
 * header("Authorization", "Bearer your_token")
 * }
 * body = """
 * {
 * "name": "Jane Doe",
 * "email": "jane.doe@example.com"
 * }
 * """.trimIndent()
 * }
 * ```
 */
fun httpRequest(block: HttpRequestBuilder.() -> Unit): HttpRequest {
    val builder = HttpRequestBuilder()
    builder.block() // Apply the configurations defined in the lambda
    return builder.build()
}
