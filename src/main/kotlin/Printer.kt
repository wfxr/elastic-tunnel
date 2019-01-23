import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.elasticsearch.search.SearchHit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min


fun output(
    result: Result,
    fields: List<String>?,
    format: OutputFormat,
    pretty: Boolean,
    finished: AtomicLong,
    remain: AtomicLong
) {
    val count = min(remain.get(), result.hits.count().toLong())
    result.hits.take(count.toInt()).let { items ->
        when (format) {
            OutputFormat.JSON -> {
                println(items.toJson(if (pretty) "  " else ""))
            }
            OutputFormat.CSV -> {
                if (fields == null) {
                    throw Exception("fields can't be null for csv format")
                }
                val csvFormat =
                    if (finished.get() == 0L)
                        CSVFormat.DEFAULT.withHeader(*fields.toTypedArray())
                    else
                        CSVFormat.DEFAULT

                val printer = CSVPrinter(System.out, csvFormat)

                items.map { it.flatten() }.forEach { hit ->
                    printer.printRecord(fields.map { field -> hit[".$field"] })
                }
            }
        }
    }
    remain.addAndGet(-count)
    finished.addAndGet(count)
}

fun SearchHit.getValue(path: String) {
    val parts = path.split(".")
    var hit: Map<*, *> = this.sourceAsMap
    var value: Any? = hit
    parts.forEach {
        hit = (value as Map<*, *>)
        value = hit[it]
    }
}

fun Map<*, *>.flatten(): MutableMap<String, Any?> =
    mutableMapOf<String, Any?>().also { flatten("", this, it) }

fun flatten(prefix: String, source: Map<*, *>, dest: MutableMap<String, Any?>) {
    source.forEach { k, v ->
        val fullKey = "$prefix.$k"
        if (v is Map<*, *>) {
            flatten(fullKey, v, dest)
        } else {
            dest[fullKey] = v
        }
    }
}
