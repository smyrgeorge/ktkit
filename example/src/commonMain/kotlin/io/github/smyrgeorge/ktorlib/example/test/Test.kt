package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.sqlx4k.ResultSet
import io.github.smyrgeorge.sqlx4k.annotation.Id
import io.github.smyrgeorge.sqlx4k.annotation.Table
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.RowMapper as IRowMapper

@Table("test")
data class Test(
    @Id(insert = true)
    val id: Int,
    val test: String,
) {
    object RowMapper : IRowMapper<Test> {
        override fun map(row: ResultSet.Row): Test {
            val id: ResultSet.Row.Column = row.get("id")
            val test: ResultSet.Row.Column = row.get("test")
            return Test(id = id.asInt(), test = test.asString())
        }
    }
}
