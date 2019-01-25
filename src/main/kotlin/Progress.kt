import java.io.PrintStream

class Progress {
    var totalFinished: Long = 0
        private set
    private val sliceFinished: MutableMap<Int, Long> = mutableMapOf()
    fun inc(sliceId: Int, count: Int = 1) {
        sliceFinished.compute(sliceId) { _, sum -> (sum ?: 0L) + count }
        totalFinished++
    }

    fun println(sliceId: Int, sliceTotal: Long, printStream: PrintStream = System.err) {
        printStream.print(" finished: $totalFinished;")
        printStream.print(" sliceId: $sliceId;")
        printStream.print(" sliceFinished: ${sliceFinished[sliceId]};")
        printStream.print(" sliceTotal: $sliceTotal;")
        printStream.println()
    }
}