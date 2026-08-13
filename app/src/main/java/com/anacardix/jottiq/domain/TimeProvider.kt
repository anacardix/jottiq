package com.anacardix.jottiq.domain

/** Wall-clock time as epoch millis UTC — an injectable seam so tests can fake "now". */
fun interface TimeProvider {
    fun nowEpochMillis(): Long
}
