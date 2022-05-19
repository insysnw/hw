package net.fennmata.amogus.terminal.client.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import net.fennmata.amogus.terminal.client.infrastructure.Serializer

@JsonClass(generator = "net.fennmata.amogus.terminal.client.models.ServerResponseJsonAdapter", generateAdapter = true)
sealed interface ServerResponse

class ServerResponseJsonAdapter(moshi: Moshi) : JsonAdapter<ServerResponse>() {

    companion object {
        private val killResultOptions = JsonReader.Options.of("killed_users_count")
        private val roleOptions = JsonReader.Options.of("title", "allowed_commands")
        private val filesListOptions = JsonReader.Options.of("files")
        private val usersListOptions = JsonReader.Options.of("users")
        private val notificationOptions = JsonReader.Options.of("message")
        private val moveToOptions = JsonReader.Options.of("location")
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ServerResponse? {
        val readerCopy = reader.peekJson()
        readerCopy.beginObject()
        return when {
            readerCopy.selectName(killResultOptions) != -1 -> {
                Serializer.moshi.adapter(KillResult::class.java).fromJson(reader)
            }
            readerCopy.selectName(roleOptions) != -1 -> {
                Serializer.moshi.adapter(Role::class.java).fromJson(reader)
            }
            readerCopy.selectName(filesListOptions) != -1 -> {
                Serializer.moshi.adapter(FilesList::class.java).fromJson(reader)
            }
            readerCopy.selectName(usersListOptions) != -1 -> {
                Serializer.moshi.adapter(UsersList::class.java).fromJson(reader)
            }
            readerCopy.selectName(notificationOptions) != -1 -> {
                Serializer.moshi.adapter(Notification::class.java).fromJson(reader)
            }
            readerCopy.selectName(moveToOptions) != -1 -> {
                Serializer.moshi.adapter(MoveTo::class.java).fromJson(reader)
            }
            else -> null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ServerResponse?) {
        TODO("Not yet implemented")
    }

}
