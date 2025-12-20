package io.github.smyrgeorge.ktorlib.error.types

class UnsupportedEnumValue(enumType: String, value: String) :
    BadRequest("Unsupported enum value '$value' for type '$enumType'")
