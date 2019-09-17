package org.hisp.dhis.rules.utils

actual typealias Date = java.util.Date

actual fun Date.compareTo(date: Date): Int {
    return this.compareTo(date)
}

actual typealias Callable<V> = java.util.concurrent.Callable<V>

actual typealias SimpleDateFormat = java.text.SimpleDateFormat

actual typealias DecimalFormat = java.text.DecimalFormat