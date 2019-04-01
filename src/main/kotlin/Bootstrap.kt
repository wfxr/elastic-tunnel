import cli.getConfig
import com.github.wfxr.kprogress.IProgressState.Companion.INDEFINITE
import com.github.wfxr.kprogress.ProgressBar
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
            val client = getClient(host, user, pass)
            val pb = ProgressBar(MutableList(slice) { INDEFINITE }, "slice")
            try {
                val source = Channel<Result>()
                val tasks = (0 until slice).map { sliceId ->
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
                }
                launch(Dispatchers.IO) {
                    tasks.forEach { it.join() }
                    source.close()
                }
                val unpacked = unpack(source, limit, pb)
                when (output) {
                    OutputFormat.JSON -> sinkJSON(unpacked, pretty)
                    OutputFormat.CSV -> sinkCSV(unpacked, fields)
                }
            } catch (e: Exception) {
            } finally {
                pb.close()
                client.close()
            }
        }
        // Explicit exit for the kotlin-coroutine issue
        // https://github.com/Kotlin/kotlinx.coroutines/issues/856
        exitProcess(0)
    }
} catch (e: Exception) {
    System.err.println("Error: ${e.message}")
    exitProcess(1)
}
