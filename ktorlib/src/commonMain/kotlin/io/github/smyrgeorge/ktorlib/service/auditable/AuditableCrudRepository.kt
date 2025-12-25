package io.github.smyrgeorge.ktorlib.service.auditable

import io.github.smyrgeorge.sqlx4k.arrow.ArrowContextCrudRepository

@OptIn(ExperimentalContextParameters::class)
interface AuditableCrudRepository<T : Auditable> : ArrowContextCrudRepository<T> {
//    override suspend fun preInsertHook(entity: T): T {
//        println("PRE INSERT: $entity")
//        return entity
//    }
//
//    override suspend fun preUpdateHook(entity: T): T {
//        println("PRE UPDATE: $entity")
//        return entity
//    }
//
//    override suspend fun preDeleteHook(entity: T): T {
//        println("PRE UPDATE: $entity")
//        return entity
//    }
}