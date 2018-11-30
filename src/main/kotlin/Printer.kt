import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

fun output(
    result: Result,
    fields: List<String>,
    limit: Int = Int.MAX_VALUE,
    format: OutputFormat,
    pretty: Boolean
) {
    when (format) {
        OutputFormat.JSON -> {
            println(result.toJson(if (pretty) "  " else ""))
        }
        OutputFormat.CSV -> {
            val printer = CSVPrinter(System.out, CSVFormat.DEFAULT.withHeader(*fields.toTypedArray()))
            result.hits.take(limit).forEach { hit ->
                printer.printRecord(fields.map { hit[it] })
            }
        }
    }
}
