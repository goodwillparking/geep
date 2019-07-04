package stately

interface Color {
    val baseEnum: BaseColor
    val hexCode: String
}

enum class BaseColor(override val hexCode: String) : Color {
    RED("ff0000"),
    BLUE("0000fff"),
    GREEN("00ff00"),
    ORANGE("ff9500"),
    PINK("fe9ea5");

    override val baseEnum = this
}

enum class RedishColor(private val baseColor: BaseColor) : Color by baseColor {
    RED(BaseColor.RED),
    ORANGE(BaseColor.ORANGE),
    PINK(BaseColor.PINK);
}

fun handleColor(color: Color): Int {
    return when (color.baseEnum) { // Underlying enum exposed for exhaustive 'when'.
        BaseColor.RED -> 1345
        BaseColor.BLUE -> 1235
        BaseColor.GREEN -> 1345
        BaseColor.ORANGE -> 1435
        BaseColor.PINK -> 1435
    }
}

fun handleRedishColor(color: RedishColor): Int {
    return when (color) {
        RedishColor.RED -> 1346
        RedishColor.ORANGE -> 1345
        RedishColor.PINK -> 12345
    }
}

fun main() {
    handleColor(RedishColor.ORANGE)
    handleColor(BaseColor.GREEN)

    handleRedishColor(RedishColor.ORANGE)
    // handleRedishColor(BaseColor.GREEN) // error
}