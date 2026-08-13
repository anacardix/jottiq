package com.anacardix.jottiq.data.local

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a block of DAO calls atomically. Repositories that write across more than one DAO (e.g. a
 * folder trash cascading into its notes) go through this instead of calling [androidx.room.RoomDatabase.withTransaction]
 * directly, so they stay testable against in-memory DAO fakes (CLAUDE.md's fakes-first policy)
 * without needing a real Room database in tests — see `FakeTransactionRunner`.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

/** Production [TransactionRunner]: wraps [block] in a real Room transaction. */
class RoomTransactionRunner @Inject constructor(
    private val database: JottiqDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
}
