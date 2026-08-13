package com.anacardix.jottiq.fakes

import com.anacardix.jottiq.data.local.TransactionRunner

/**
 * No-op [TransactionRunner] fake: runs [block] directly with no real transaction, since fake DAOs
 * are plain in-memory state with no database to open one against (CLAUDE.md's fakes-first policy).
 */
class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
