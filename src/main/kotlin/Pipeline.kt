@file:Suppress("EXPERIMENTAL_API_USAGE")

import com.github.wfxr.kprogress.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.slice.SliceBuilder
import java.io.PrintStream

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
    try {
        var res = client.search(query, index, size, keepAlive, fields) {
            if (slice > 1) slice(SliceBuilder(sliceId, slice)) else this
        }.apply { this.sliceId = sliceId }

        while (res.hits.isNotEmpty()) {
            channel.send(res)
            res = client.scroll(res.scrollId, keepAlive).apply { this.sliceId = sliceId }
        }
    } catch (e: Exception) {
        when {
            channel.isClosedForSend -> return
            else -> throw e
        }
    }
}

fun CoroutineScope.unpack(
    source: ReceiveChannel<Result>,
    total: Long? = null,
    pb: ProgressBar
) = produce {
    for (res in source) {
        pb[res.sliceId].updateTotal(res.total)
        for (hit in res.hits) {
            send(hit)
            pb[res.sliceId].inc()
            total?.takeIf { pb.curr() >= it }?.let {
                source.cancel()
                return@produce
            }
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun sinkCSV(
    hits: ReceiveChannel<Hit>,
    fields: List<String>?,
    os: PrintStream
) {
    require(fields != null) {
        "fields can't be null for csv format"
    }

    os.println(fields.joinToString(","))
    val csvFormat = CSVFormat.DEFAULT
    val printer = CSVPrinter(System.out, csvFormat)

    hits.consumeEach {
        val flatten = it.flatten()
        printer.printRecord(fields.map { field -> flatten[".$field"] })
    }
}

suspend fun sinkJSON(
    hits: ReceiveChannel<Hit>,
    pretty: Boolean,
    os: PrintStream
) {
    hits.consumeEach {
        os.println(it.toJson(pretty))
    }
}