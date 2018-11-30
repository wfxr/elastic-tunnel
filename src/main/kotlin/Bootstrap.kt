import cli.getConfig
import org.elasticsearch.index.query.QueryBuilders
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
            var remain = config.limit
            var res = c.search(query, config.index, config.scrollSize, config.scrollTimeout, config.fields)
            while (res.hits.isNotEmpty() && remain > 0) {
                output(res, config.fields, remain, config.output, config.pretty)
                res = c.scroll(res.scrollId, config.scrollTimeout)
                remain -= res.hits.size
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    }
}

