package net.fennmata.cnt.lab1.common

sealed interface UserCommand<T : Application<T>> {
    fun executeOn(application: Application<T>, arg: String? = null)
}

class NoUserCommand<T : Application<T>> : UserCommand<T> {
    override fun executeOn(application: Application<T>, arg: String?) {
        ErrorChatOutput.write("Please enter a command.")
    }
}

class UnknownUserCommand<T : Application<T>> : UserCommand<T> {
    override fun executeOn(application: Application<T>, arg: String?) {
        ErrorChatOutput.write("Unknown command.")
    }
}

sealed interface CustomUserCommand<T : Application<T>> : UserCommand<T> {
    val name: String
}
