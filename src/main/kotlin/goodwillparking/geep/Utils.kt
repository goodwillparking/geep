package goodwillparking.geep

internal inline fun <T> Iterable<T>.exists(predicate: (T) -> Boolean): Boolean {
    for (element in this) {
        if (predicate(element)) {
            return true
        }
    }
    return false
}

internal inline fun <T> Iterable<T>.indexWhere(predicate: (T) -> Boolean): Int? {
    for ((i, element) in this.withIndex()) {
        if (predicate(element)) {
            return i
        }
    }
    return null
}
