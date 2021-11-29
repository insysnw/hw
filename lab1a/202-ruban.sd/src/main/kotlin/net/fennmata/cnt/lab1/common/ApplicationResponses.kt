package net.fennmata.cnt.lab1.common

interface ApplicationBaseResponse<T : Application<T>> {
    suspend fun execute(arg: String? = null)
}

interface ApplicationResponse<T : Application<T>> : ApplicationBaseResponse<T> {
    val command: String
}
