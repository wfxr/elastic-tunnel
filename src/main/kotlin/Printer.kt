import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.util.concurrent.atomic.AtomicInteger

fun output(
    result: Result,
    fields: List<String>,
    format: OutputFormat,
    pretty: Boolean,
    finished: AtomicInteger = AtomicInteger(0),
    remain: AtomicInteger = AtomicInteger(Int.MAX_VALUE)
) {
    result.hits.take(remain.get()).let { items ->
        when (format) {
            OutputFormat.JSON -> {
                println(items.toJson(if (pretty) "  " else ""))
            }
            OutputFormat.CSV -> {
                val csvFormat =
                    if (finished.get() == 0)
                        CSVFormat.DEFAULT.withHeader(*fields.toTypedArray())
                    else
                        CSVFormat.DEFAULT

                val printer = CSVPrinter(System.out, csvFormat)

                items.forEach { hit ->
                    printer.printRecord(fields.map { hit[it] })
                }
            }
        }
        remain.addAndGet(-items.size)
        finished.addAndGet(items.size)
    }
}
