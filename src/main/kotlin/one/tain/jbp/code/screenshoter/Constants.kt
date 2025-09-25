package one.tain.jbp.code.screenshoter

import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * TODO: Provide configuration option for date time pattern
 */
val DATE_TIME_PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

/**
 * TODO: Provide configuration option for size limit warning
 */
const val SIZE_LIMIT_TO_WARN: Long = 3000000L


val EMPTY_SUFFIX: Pattern = Pattern.compile("\n\\s+$")
