package cli

import OutputFormat
import java.nio.file.Path

data class Config(
    val host: String,
    val user: String,
    val pass: String,
    val index: String,
    val query: Path,
    val fields: List<String>?,
    val limit: Long,
    val output: OutputFormat,
    val pretty: Boolean,
    val scrollSize: Int,
    val scrollTimeout: String
)