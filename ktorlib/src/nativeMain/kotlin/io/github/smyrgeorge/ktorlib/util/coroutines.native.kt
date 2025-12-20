package io.github.smyrgeorge.ktorlib.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val Dispatchers.IO_DISPATCHER: CoroutineDispatcher
    get() = Dispatchers.IO