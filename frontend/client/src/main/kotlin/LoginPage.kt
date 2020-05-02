import components.appButton
import components.loadingComponent
import components.mobileView
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
import services.putInCache
import styled.*
import kotlin.browser.window

interface LoginPageProps: RProps, CacheProps

interface LoginPageState: RState {
    var username: String
    var password: String
    var errorMessage: String?
    var isLoading: Boolean
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
            setState {
                isLoading = false
            }
            when {
                it.status.isSuccess() -> {
                    val encrypted = it.receive<EncryptedUsername>()
                    window.caches.putInCache(DATA_CACHE, key = "username", data = encrypted.username.contentToString())
                    props.setCredentialsExisting(true)
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
                mobileView {
                    flexDirection = FlexDirection.column
                }
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
            styledDiv {
                css {
                    mobileView {
                        textAlign = TextAlign.center
                        marginBottom = LinearDimension("50px")
                    }
                }
                +"Please enter your KTU network username and password to proceed."
            }

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
                            if (state.username.isNotBlank() && state.password.isNotBlank()) {
                                setState {
                                    isLoading = true
                                }
                                authenticateUser()
                            }
                        }
                    }
                }
            }
            when {
                state.isLoading -> loadingComponent()
                state.errorMessage.isNullOrEmpty().not() -> styledDiv {
                    css {
                        color = Color.tomato
                        marginTop = LinearDimension("20px")
                        textAlign = TextAlign.center
                    }
                    state.errorMessage?.let {+it}
                }
            }
        }
    }
}

fun RBuilder.loginPage(handler: LoginPageProps.() -> Unit): ReactElement {
    return child(LoginPage::class) {
        this.attrs(handler)
    }
}

