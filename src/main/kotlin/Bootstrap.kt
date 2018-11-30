import cli.getConfig
import org.elasticsearch.index.query.QueryBuilders
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = try {
        getConfig(args)
    } catch (e: Exception) {
        exitProcess(1)
    }

    try {
        val client = getClient(config.host, config.user, config.pass)
        client.use { c ->
            val queryJson = System.`in`.bufferedReader().use { it.readText() }
            val query = QueryBuilders.wrapperQuery(queryJson)
            val remain = AtomicInteger(config.limit)
            val curr = AtomicInteger(0)
            var res = c.search(query, config.index, config.scrollSize, config.scrollTimeout, config.fields)
            while (res.hits.isNotEmpty() && remain.get() > 0) {
                output(res, config.fields, remain, curr, config.output, config.pretty)
                res = c.scroll(res.scrollId, config.scrollTimeout)
                System.err.println("progress: $curr / ${min(res.total, config.limit.toLong())}")
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    }
}

