package fr.deuspheara.oree.core.domain.concurrency

import kotlinx.coroutines.CoroutineDispatcher

class AppDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher)
