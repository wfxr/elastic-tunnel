import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

fun output(
    result: Result,
    fields: List<String>,
    format: OutputFormat,
    pretty: Boolean,
    finished: AtomicLong,
    remain: AtomicLong
) {
    val count = min(remain.get(), result.hits.size.toLong())
    result.hits.take(count.toInt()).let { items ->
        when (format) {
            OutputFormat.JSON -> {
                println(items.toJson(if (pretty) "  " else ""))
            }
            OutputFormat.CSV -> {
                val csvFormat =
                    if (finished.get() == 0L)
                        CSVFormat.DEFAULT.withHeader(*fields.toTypedArray())
                    else
                        CSVFormat.DEFAULT

                val printer = CSVPrinter(System.out, csvFormat)

                items.forEach { hit ->
                    printer.printRecord(fields.map { hit[it] })
                }
            }
        }
    }
    remain.addAndGet(-count)
    finished.addAndGet(count)
}
