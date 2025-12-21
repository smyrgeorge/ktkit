package io.github.smyrgeorge.ktorlib.example

interface UserRepository {
    suspend fun findById(id: String): UserDto?
    suspend fun search(query: String, limit: Int): List<UserDto>
}

class UserRepositoryImpl : UserRepository {
    // Mock implementation - replace with actual database access
    private val users = listOf(
        UserDto("1", "Alice", "alice@example.com", listOf("USER", "ADMIN")),
        UserDto("2", "Bob", "bob@example.com", listOf("USER")),
        UserDto("3", "Charlie", "charlie@example.com", listOf("USER"))
    )

    override suspend fun findById(id: String): UserDto? {
        return users.find { it.id == id }
    }

    override suspend fun search(query: String, limit: Int): List<UserDto> {
        return users
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(limit)
    }
}
