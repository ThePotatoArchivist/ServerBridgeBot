package archives.tater.bot.bridge

import kotlinx.io.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@OptIn(ExperimentalSerializationApi::class)
class SaveData<T>(val file: Path, var value: T) {
    companion object {
        inline fun <reified T> load(file: Path, default: () -> T) = SaveData(
            file,
            try { file.inputStream() } catch (_: IOException) { null }
                ?.let { Json.decodeFromStream<T>(it) }
                ?: default()
        )

        inline fun <reified T> SaveData<T>.save() {
            Json.encodeToStream<T>(value, file.outputStream())
        }

        inline fun <reified T> SaveData<T>.update(action: (T) -> Unit) {
            action(value)
            save()
        }
    }
}