package com.pharmatrade.core.network

import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger

internal actual val httpLogger: Logger = Logger.DEFAULT
