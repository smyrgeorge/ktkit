package io.github.smyrgeorge.ktorlib.util.extentions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IO_DISPATCHER: CoroutineDispatcher
    get() = Dispatchers.IO