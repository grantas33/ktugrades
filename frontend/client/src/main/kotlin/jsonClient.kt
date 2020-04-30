import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val json = Json(configuration = JsonConfiguration.Stable)

val jsonClient = HttpClient {
    expectSuccess = false
    install(JsonFeature) {
        serializer = KotlinxSerializer(json = json)
    }
}