package cli

import OutputOption
import org.apache.commons.cli.*
import kotlin.math.min

fun getConfig(args: Array<String>): Config {
    val cli = try {
        DefaultParser().parse(options, args).validate()
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}\n")
        val formatter = HelpFormatter()
        val footer = """|
            |The MIT License
            |Copyright(c) 2018 Wenxuan Zhang
        """.trimMargin()
        formatter.printHelp("elastic-tunnel", null, options, footer, true)
        throw e
    }

    val host = cli.getOptionValue("host")
    val user = cli.getOptionValue("user") ?: "Anonymous"
    val pass = cli.getOptionValue("pass") ?: "Anonymous"
    val index = cli.getOptionValue("index")
    val fields = cli.getOptionValue("fields").split(",")
    val limit = cli.getOptionValue("limit")?.toInt() ?: Int.MAX_VALUE
    val output = (cli.getOption("output") as OutputOption).getEnum()
    val pretty = cli.hasOption("pretty")
    val scrollSize = min(min(cli.getOptionValue("size")?.toInt() ?: 2000, limit), 10000)
    val scrollTimeout = cli.getOptionValue("scroll") ?: "1m"

    return Config(
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
            .required()
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
            .type(Int::class.java)
            .desc("Max entries to download")
            .hasArg()
            .build()
    )
    .addOption(
        Option.builder("f")
            .longOpt("fields")
            .desc("The source fields to download")
            .hasArg()
            .required()
            .build()
    )
    .addOption(
        Option.builder()
            .longOpt("pretty")
            .desc("Pretty printing")
            .hasArg(false)
            .build()
    )!!
