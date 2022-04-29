package tech.ghenkle.kumquatt.utils

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private val UTF8: Charset = StandardCharsets.UTF_8

/**
 * Converts an array of bytes into a UTF-8 encoded string.
 */
fun ByteArray.utf8(): String = toString(UTF8)

/**
 * Converts a UTF-8 encoded string into an array of bytes.
 */
fun String.utf8Bytes(): ByteArray = toByteArray(UTF8)