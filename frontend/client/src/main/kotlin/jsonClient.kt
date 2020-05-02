import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import org.ktugrades.common.jsonSerializer

val jsonClient = HttpClient {
    expectSuccess = false
    install(JsonFeature) {
        serializer = KotlinxSerializer(json = jsonSerializer)
    }
}