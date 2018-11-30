import org.apache.commons.cli.Option

class OutputOption(
    argName: String? = null,
    opt: String? = null,
    longOpt: String? = null,
    isRequired: Boolean = false,
    description: String = ""
) :
    Option(opt, longOpt, true, "$description [${OutputFormat.values().joinToString("|")}]") {
    init {
        this.isRequired = isRequired
        this.argName = argName
    }
    fun getEnum(): OutputFormat {
        return OutputFormat.valueOf(this.getValue(0).toUpperCase())
    }
}
