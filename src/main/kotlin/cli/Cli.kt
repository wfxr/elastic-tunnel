package cli

import OutputOption
import io.github.cdimascio.dotenv.dotenv
import org.apache.commons.cli.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.max
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
        val query = Paths.get(cli.getOptionValue("query"))
        val slice = cli.getOptionValue("slice")?.toInt() ?: 1
        val fields = cli.getOptionValue("fields")?.split(",")
        val limit = cli.getOptionValue("limit")?.toLong()
        val output = (cli.getOption("output") as OutputOption).getEnum()
        val pretty = cli.hasOption("pretty")
        val scrollSize = listOf(
            cli.getOptionValue("size")?.toLong() ?: env["SCROLL_SIZE"]?.toLong(),
            limit,
            10000
        ).mapNotNull { it }.min()!!.toInt()
        val scrollTimeout = cli.getOptionValue("scroll") ?: env["SCROLL_SIZE"] ?: "1m"

        if (fields == null && output == OutputFormat.CSV) {
            throw Exception("fields must be specified when output format is csv")
        }

        Config(
            host = host,
            user = user,
            pass = pass,
            index = index,
            query = query,
            slice = slice,
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
            |Copyright(c) 2019 Wenxuan Zhang
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
            .desc("Elasticsearch index name")
            .hasArg()
            .required()
            .build()
    )
    .addOption(
        Option.builder("q")
            .longOpt("query")
            .desc("Query file")
            .hasArg()
            .required()
            .build()
    )
    .addOption(
        Option.builder("s")
            .longOpt("slice")
            .desc("Elasticsearch Scroll slice")
            .hasArg()
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
            .desc("Fields to download")
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
