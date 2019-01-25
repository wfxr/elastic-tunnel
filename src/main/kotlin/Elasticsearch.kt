@file:Suppress("EXPERIMENTAL_API_USAGE")

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.slice.SliceBuilder
import kotlin.math.min


fun getClient(host: String, user: String, pass: String) =
    RestHighLevelClient(RestClient.builder(HttpHost.create(host))
        .setHttpClientConfigCallback { builder ->
            builder.setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials(user, pass))
            })
        }
    )

fun RestHighLevelClient.search(
    query: QueryBuilder,
    index: String,
    size: Int,
    keepAlive: String,
    fields: List<String>?,
    extraBuilder: SearchSourceBuilder.() -> SearchSourceBuilder = { this }
) = search(
    SearchRequest(index).source(
        SearchSourceBuilder()
            .query(query)
            .size(size)
            .apply { fields?.toTypedArray()?.let { fetchSource(it, null) } }
            .extraBuilder()
    ).scroll(keepAlive)
).toRecords()

fun RestHighLevelClient.scroll(
    scrollId: String,
    keepAlive: String
) = searchScroll(
    SearchScrollRequest(scrollId).scroll(keepAlive)
).toRecords()

fun SearchResponse.toRecords() = Result(
    hits.totalHits,
    took.millis,
    hits.map { it.sourceAsMap },
    scrollId ?: ""
)

typealias Hit = Map<String, Any>

data class Result(
    val total: Long,
    val took: Long,
    val hits: List<Hit>,
    @Transient val scrollId: String = ""
)

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
    }
    while (res.hits.isNotEmpty()) {
        channel.send(res)
        try {
            res = withContext(Dispatchers.IO) {
                client.scroll(res.scrollId, keepAlive)
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

fun CoroutineScope.unpack(results: ReceiveChannel<Result>, limit: Long) = produce {
    var curr = 0L
    for (res in results) {
        for (hit in res.hits) {
            send(hit)
            ++curr
            System.err.println("progress: $curr / ${min(res.total, limit)}")
            if (curr >= limit) {
                results.cancel()
                return@produce
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
