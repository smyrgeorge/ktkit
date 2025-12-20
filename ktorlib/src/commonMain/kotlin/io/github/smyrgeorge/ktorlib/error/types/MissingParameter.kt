package io.github.smyrgeorge.ktorlib.error.types

class MissingParameter(type: String, name: String) :
    BadRequest("Missing required parameter '$name' of type '$type'")
