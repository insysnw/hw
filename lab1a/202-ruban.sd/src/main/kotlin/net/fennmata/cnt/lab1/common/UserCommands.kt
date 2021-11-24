package net.fennmata.cnt.lab1.common

interface UserCommand<T : Application<T>> {
    suspend fun executeOn(application: Application<T>, arg: String? = null)
}

class NoUserCommand<T : Application<T>> : UserCommand<T> {
    override suspend fun executeOn(application: Application<T>, arg: String?) {
        ErrorChatOutput.write("Please enter a command.")
    }
}

class UnknownUserCommand<T : Application<T>> : UserCommand<T> {
    override suspend fun executeOn(application: Application<T>, arg: String?) {
        ErrorChatOutput.write("Unknown command.")
    }
}

interface CustomUserCommand<T : Application<T>> : UserCommand<T> {
    val name: String
}
