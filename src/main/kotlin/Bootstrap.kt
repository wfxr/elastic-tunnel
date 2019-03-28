import cli.getConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.elasticsearch.index.query.QueryBuilders
import java.nio.file.Files
import kotlin.system.exitProcess

@Suppress("BlockingMethodInNonBlockingContext", "RemoveExplicitTypeArguments")
fun main(args: Array<String>) = try {
    runBlocking<Unit> {
        val config = getConfig(args)
        with(config) {
            val queryJson = Files.newBufferedReader(query).use { it.readText() }
            val query = QueryBuilders.wrapperQuery(queryJson)
            getClient(host, user, pass).use { client ->
                val source = Channel<Result>()
                launch {
                    (0 until slice).map { sliceId ->
                        launch(Dispatchers.IO) {
                            source(
                                source,
                                client,
                                query,
                                index,
                                scrollSize,
                                slice,
                                sliceId,
                                scrollTimeout,
                                fields
                            )
                        }
                    }.forEach { it.join() }.also {
                        source.close()
                    }
                }
                val unpacked = unpack(source, limit)
                when (output) {
                    OutputFormat.JSON -> sinkJSON(unpacked, pretty)
                    OutputFormat.CSV -> sinkCSV(unpacked, fields)
                }
                // Explicit exit for the kotlin-coroutine issue
                // https://github.com/Kotlin/kotlinx.coroutines/issues/856
                exitProcess(0)
            }
        }
    }
} catch (e: Exception) {
    System.err.println("Error: ${e.message}")
    exitProcess(1)
}
