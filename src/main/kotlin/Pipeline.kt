@file:Suppress("EXPERIMENTAL_API_USAGE")

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.slice.SliceBuilder

suspend fun source(
    channel: SendChannel<Result>,
    client: RestHighLevelClient,
    query: QueryBuilder,
    index: String,
    size: Int,
    slice: Int,
    sliceId: Int,
    keepAlive: String,
    fields: List<String>?
) {
    var res = withContext(Dispatchers.IO) {
        client.search(query, index, size, keepAlive, fields) {
            if (slice > 1) slice(SliceBuilder(sliceId, slice)) else this
        }
    }.apply { this.sliceId = sliceId }
    while (res.hits.isNotEmpty()) {
        channel.send(res)
        try {
            res = withContext(Dispatchers.IO) {
                client.scroll(res.scrollId, keepAlive).apply { this.sliceId = sliceId }
            }
        } catch (e: Exception) {
            if (!channel.isClosedForSend) {
                throw e
            } else {
                return
            }
        }
    }
}

fun CoroutineScope.unpack(results: ReceiveChannel<Result>, limit: Long?) = produce {
    val progress = Progress()
    for (res in results) {
        for (hit in res.hits) {
            send(hit)
            progress.inc(res.sliceId)
            if (limit != null && progress.totalFinished >= limit) {
                progress.println(res.sliceId, res.total)
                results.cancel()
                return@produce
            }
        }
        progress.println(res.sliceId, res.total)
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun sinkCSV(
    hits: ReceiveChannel<Hit>,
    fields: List<String>?
) {
    if (fields == null) throw Exception("fields can't be null for csv format")

    println(fields.joinToString(","))
    val csvFormat = CSVFormat.DEFAULT
    val printer = CSVPrinter(System.out, csvFormat)

    for (hit in hits) {
        val flatten = hit.flatten()
        printer.printRecord(fields.map { field -> flatten[".$field"] })
    }
}

suspend fun sinkJSON(
    hits: ReceiveChannel<Hit>,
    pretty: Boolean
) {
    for (hit in hits) {
        println(hit.toJson(if (pretty) "  " else ""))
    }
}