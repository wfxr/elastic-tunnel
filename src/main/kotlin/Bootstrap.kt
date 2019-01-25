import cli.getConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
        with(config) {
            getClient(host, user, pass).use { client ->
                val source = Channel<Result>().let { channel ->
                    for (sliceId in 0 until slice) {
                        launch {
                            source(
                                channel,
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
                    };channel
                }
                val unpacked = unpack(source, limit)
                when (output) {
                    OutputFormat.JSON -> sinkJSON(unpacked, pretty)
                    OutputFormat.CSV -> sinkCSV(unpacked, fields)
                }
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    }
    // Explicit exit for the kotlin-coroutine issue
    // https://github.com/Kotlin/kotlinx.coroutines/issues/856
    exitProcess(0)
}

