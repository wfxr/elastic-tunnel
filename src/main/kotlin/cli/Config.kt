package cli

import OutputFormat

data class Config(
    val host: String,
    val user: String,
    val pass: String,
    val index: String,
    val fields: List<String>,
    val limit: Int,
    val output: OutputFormat,
    val pretty: Boolean,
    val scrollSize: Int,
    val scrollTimeout: String
)