package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

/**
 * Abstract base class for 400 Bad Request errors.
 */
abstract class BadRequest(message: String) : Error(message, HttpStatus.BAD_REQUEST)

/**
 * Concrete implementation of a Bad Request error.
 */
class BadRequestImpl(message: String) : BadRequest(message)

/**
 * Error thrown when a required parameter is missing.
 */
class MissingParameter(type: String, name: String) :
    BadRequest("Missing required parameter '$name' of type '$type'")

/**
 * Error thrown when an enum value is not supported.
 */
class UnsupportedEnumValue(enumType: String, value: String) :
    BadRequest("Unsupported enum value '$value' for type '$enumType'")

/**
 * Error thrown when validation fails.
 */
class ValidationError(message: String) : BadRequest(message)

/**
 * Error thrown when the authorization header cannot be parsed.
 */
class CannotParseAuthorizationHeader(message: String) : BadRequest(message)
