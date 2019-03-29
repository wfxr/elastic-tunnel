@file:Suppress("EXPERIMENTAL_API_USAGE")

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
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
): Pair<Long, Job> {
    return try {
        var res = client.search(query, index, size, keepAlive, fields) {
            if (slice > 1) slice(SliceBuilder(sliceId, slice)) else this
        }.apply { this.sliceId = sliceId }

        res.total to CoroutineScope(Dispatchers.IO).launch {
            while (res.hits.isNotEmpty()) {
                channel.send(res)
                res = client.scroll(res.scrollId, keepAlive).apply { this.sliceId = sliceId }
            }
        }
    } catch (e: Exception) {
        when {
            channel.isClosedForSend -> return 0L to CoroutineScope(Dispatchers.IO).launch {}
            else -> throw e
        }
    }
}

fun CoroutineScope.unpack(source: ReceiveChannel<Result>, total: Long) = produce {
    ProgressBarBuilder()
        .setInitialMax(total)
        .setPrintStream(System.err)
        .setTaskName("Progress")
        .setStyle(ProgressBarStyle.UNICODE_BLOCK)
        .setUnit(" docs", 1)
        .setUpdateIntervalMillis(50)
        .showSpeed("0")
        .build()
        .use { pb ->
            for (res in source) {
                for (hit in res.hits) {
                    send(hit)
                    pb.step()
                    if (pb.current >= total) {
                        source.cancel()
                        return@produce
                    }
                }
            }
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