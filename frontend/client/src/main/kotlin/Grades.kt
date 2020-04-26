import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.list
import org.ktugrades.common.CommonDateTime
import org.ktugrades.common.EncryptedUsername
import org.ktugrades.common.ErrorMessage
import org.ktugrades.common.MarkInfoResponse
import org.w3c.fetch.RequestInit
import react.*
import react.dom.h1
import kotlin.browser.window

interface GradesProps: RProps, LocalStorageProps

interface GradesState: RState {
    var markState: MarkState
}

class Grades: RComponent<GradesProps, GradesState>() {

    override fun GradesState.init() {
        markState = MarkState.Loading

        MainScope().launch {
            window.fetch("${SERVER_URL}/grades", RequestInit(
                method = "POST",
                headers = applicationJsonHeaders,
                body = json.stringify(EncryptedUsername.serializer(), EncryptedUsername(username = getUsername()!!))
            )).await().let {
                if (it.ok) {
                    val marks = json.parse(MarkInfoResponse.serializer().list, it.text().await())
                    setState {
                        markState = MarkState.Success(marks = marks)
                    }
                } else {
                    val error = json.parse(ErrorMessage.serializer(), it.text().await())
                    setState {
                        markState = MarkState.Error(error = error)
                    }
                }
            }
        }
    }

    override fun RBuilder.render() {
        when (state.markState) {
            is MarkState.Success ->  h1 {
                +"Grades"
            }

            is MarkState.Error -> h1 {
                + (state.markState as MarkState.Error).error.message
            }
            is MarkState.Loading -> loadingComponent()
        }
    }
}

fun RBuilder.grades(handler: GradesProps.() -> Unit): ReactElement {
    return child(Grades::class) {
        this.attrs(handler)
    }
}

