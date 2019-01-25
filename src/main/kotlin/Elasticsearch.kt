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
    @Transient val scrollId: String = "",
    @Transient var sliceId: Int = 0
)

