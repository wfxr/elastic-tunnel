import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

fun output(
    result: Result,
    fields: List<String>,
    limit: Int = Int.MAX_VALUE,
    format: OutputFormat,
    pretty: Boolean
) {
    result.hits.take(limit).let { items ->
        when (format) {
            OutputFormat.JSON -> {
                println(items.toJson(if (pretty) "  " else ""))
            }
            OutputFormat.CSV -> {
                val printer = CSVPrinter(System.out, CSVFormat.DEFAULT.withHeader(*fields.toTypedArray()))
                items.forEach { hit ->
                    printer.printRecord(fields.map { hit[it] })
                }
            }
        }
    }
}
