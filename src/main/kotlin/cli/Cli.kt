package cli

import OutputOption
import io.github.cdimascio.dotenv.dotenv
import org.apache.commons.cli.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.min


val env = dotenv {
    val configDir =
        Paths.get(".esconfig").takeIf { Files.isReadable(it) }?.let { "." }
            ?: Paths.get(System.getProperty("user.home"))
    directory = configDir.toString()
    filename = ".esconfig"
    ignoreIfMissing = true
}

fun getConfig(args: Array<String>): Config {
    return try {
        val cli = DefaultParser().parse(options, args).validate()

        val host = cli.getOptionValue("host") ?: env["ES_HOST"] ?: "http://127.0.0.1:9200"
        val user = cli.getOptionValue("user") ?: env["ES_USER"] ?: "Anonymous"
        val pass = cli.getOptionValue("pass") ?: env["ES_PASS"] ?: "Anonymous"
        val index = cli.getOptionValue("index")
        val fields = cli.getOptionValue("fields")?.split(",")
        val limit = cli.getOptionValue("limit")?.toLong() ?: Long.MAX_VALUE
        val output = (cli.getOption("output") as OutputOption).getEnum()
        val pretty = cli.hasOption("pretty")
        val scrollSize =
            min(min(cli.getOptionValue("size")?.toLong() ?: env["SCROLL_SIZE"]?.toLong() ?: 2000, limit), 10000).toInt()
        val scrollTimeout = cli.getOptionValue("scroll") ?: env["SCROLL_SIZE"] ?: "1m"

        if (fields == null && output == OutputFormat.CSV) {
            throw Exception("fields must be specified when output format is csv")
        }

        Config(
            host = host,
            user = user,
            pass = pass,
            index = index,
            fields = fields,
            limit = limit,
            output = output,
            pretty = pretty,
            scrollSize = scrollSize,
            scrollTimeout = scrollTimeout
        )
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}\n")
        val formatter = HelpFormatter()
        val footer = """|
            |The MIT License
            |Copyright(c) 2018 Wenxuan Zhang
            |https://github.com/wfxr/elastic-tunnel
        """.trimMargin()
        formatter.printHelp("elastic-tunnel", null, options, footer, true)
        throw e
    }
}

fun CommandLine.validate(handler: CommandLine.() -> Unit = {}) = apply {
    options.forEach {
        val optName = it.longOpt ?: it.opt
        try {
            when (it) {
                is OutputOption -> it.getEnum()
                is Option -> {
                    if (it.hasArg() || it.isRequired)
                        getParsedOptionValue(optName) ?: throw Exception("null")
                }
            }
        } catch (e: Exception) {
            throw ParseException("Option '$optName' parse failed. Message: ${e.message}")
        }
    }
    handler()
}

fun CommandLine.getOption(opt: String) =
    options.find { it.opt == opt || it.longOpt == opt }

val options = Options()
    .addOption(
        Option.builder("h")
            .longOpt("host")
            .desc("Elasticsearch host url")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder("u")
            .longOpt("user")
            .desc("Elasticsearch user name")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder("p")
            .longOpt("pass")
            .desc("Elasticsearch user password")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder("i")
            .longOpt("index")
            .desc("Elasticsearch index name or alias")
            .hasArg()
            .required()
            .build()
    )
    .addOption(
        Option.builder()
            .longOpt("size")
            .desc("Elasticsearch scroll size")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder()
            .longOpt("scroll")
            .desc("Elasticsearch scroll timeout")
            .hasArg()
            .build()
    )
    .addOption(
        OutputOption(
            opt = "o",
            longOpt = "output",
            isRequired = true,
            description = "Output format"
        )
    )
    .addOption(
        Option.builder("l")
            .longOpt("limit")
            .desc("Max entries to download")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder("f")
            .longOpt("fields")
            .desc("The source fields to download")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder()
            .longOpt("pretty")
            .desc("Pretty printing")
            .hasArg(false)
            .build()
    )!!
