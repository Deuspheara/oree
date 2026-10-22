package fr.deuspheara.oree.core.domain.concurrency

fun interface Clock {
    fun nowMillis(): Long
}
