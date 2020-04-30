import components.appButton
import io.ktor.client.call.receive
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.ktugrades.common.EncryptedUsername
import org.ktugrades.common.Credentials
import kotlinx.coroutines.*
import kotlinx.css.*
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.ktugrades.common.ErrorMessage
import org.ktugrades.common.Routes
import org.w3c.dom.HTMLInputElement
import react.*
import styled.*
import kotlin.browser.localStorage

interface LoginPageProps: RProps, LocalStorageProps

interface LoginPageState: RState {
    var username: String
    var password: String
    var errorMessage: String?
}

class LoginPage: RComponent<LoginPageProps, LoginPageState>() {

    override fun LoginPageState.init() {
        username = ""
        password = ""
    }

    private fun authenticateUser() = GlobalScope.launch {
        jsonClient.post<HttpResponse>(SERVER_URL + Routes.Authenticate) {
            contentType(ContentType.Application.Json)
            body = Credentials(username = state.username, password = state.password)
        }.let {
            when {
                it.status.isSuccess() -> {
                    val encrypted = it.receive<EncryptedUsername>()
                    localStorage.setItem("username", encrypted.username.contentToString())
                    props.notifyLocalStorageUpdated()
                }
                else -> {
                    val error = it.receive<ErrorMessage>()
                    setState {
                        errorMessage = error.message
                    }
                }
            }
        }
    }

    private fun RBuilder.loginInput(label: String, inputType: InputType, onValueChange: (value: String) -> Unit) =
        styledDiv {
            css {
                marginTop = LinearDimension("5px")
                display = Display.flex
                justifyContent = JustifyContent.center
                width = LinearDimension("100%")
            }
            styledLabel {
                +label
                css {
                    width = LinearDimension("20%")
                }
            }
            styledInput {
                css {
                    padding = "5px"
                    outline = Outline.none
                    borderWidth = LinearDimension("0 0 2px")
                    fontSize = LinearDimension("1.5rem")
                }
                attrs {
                    type = inputType
                    onChangeFunction = {
                        val target = it.target as HTMLInputElement
                        onValueChange(target.value)
                    }
                }
            }
        }

    override fun RBuilder.render() {
        styledForm {
            css {
                display = Display.flex
                flexDirection = FlexDirection.column
                alignItems = Align.center
                fontSize = LinearDimension("1.8rem")
            }
            +"Please enter your KTU network username and password to proceed."
            loginInput(label = "Username", inputType = InputType.text) {
                setState { username = it }
            }
            loginInput(label = "Password", inputType = InputType.password) {
                setState { password = it }
            }
            styledDiv {
                css {
                    marginTop = LinearDimension("20px")
                }
                appButton {
                    +"Authenticate"
                    attrs {
                        type = ButtonType.submit
                        onClickFunction = {
                            it.preventDefault()
                            if (state.username.isNotBlank() && state.password.isNotBlank()) authenticateUser()
                        }
                    }
                }
            }
            styledDiv {
                css {
                    color = Color.tomato
                    marginTop = LinearDimension("20px")
                }
                state.errorMessage?.let { +it }
            }
        }
    }
}

fun RBuilder.loginPage(handler: LoginPageProps.() -> Unit): ReactElement {
    return child(LoginPage::class) {
        this.attrs(handler)
    }
}

