import cli.getConfig
import kotlinx.coroutines.runBlocking
import org.elasticsearch.index.query.QueryBuilders
import java.nio.file.Files
import kotlin.system.exitProcess

@Suppress("BlockingMethodInNonBlockingContext")
fun main(args: Array<String>) = runBlocking<Unit> {
    val config = try {
        getConfig(args)
    } catch (e: Exception) {
        exitProcess(1)
    }

    try {
        val queryJson = Files.newBufferedReader(config.query).use { it.readText() }
        val query = QueryBuilders.wrapperQuery(queryJson)
        getClient(config.host, config.user, config.pass).use { client ->
            val source =
                source(client, query, config.index, config.scrollSize, config.scrollTimeout, config.fields)
            val unpacked = unpack(source, config.limit)
            when (config.output) {
                OutputFormat.JSON -> sinkJSON(unpacked, config.pretty)
                OutputFormat.CSV -> sinkCSV(unpacked, config.fields)
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    }
    // Explicit exit for the kotlin-coroutine issue: https://github.com/Kotlin/kotlinx.coroutines/issues/856
    exitProcess(0)
}

