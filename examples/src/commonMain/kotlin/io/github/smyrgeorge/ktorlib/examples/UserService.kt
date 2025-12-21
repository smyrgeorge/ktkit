package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.log4k.Logger

interface UserService {
    suspend fun getUserById(id: String): UserDto
    suspend fun searchUsers(query: String, limit: Int): List<UserDto>
}

class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    private val log = Logger.of(this::class)

    override suspend fun getUserById(id: String): UserDto {
        log.info("Fetching user with id: $id")
        return userRepository.findById(id) ?: throw UserNotFoundException(id)
    }

    override suspend fun searchUsers(query: String, limit: Int): List<UserDto> {
        log.info("Searching users with query: $query, limit: $limit")
        return userRepository.search(query, limit)
    }
}

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val roles: List<String>
)

class UserNotFoundException(id: String) : Exception("User not found: $id")
