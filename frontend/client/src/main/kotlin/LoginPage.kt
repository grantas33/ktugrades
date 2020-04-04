import org.ktugrades.common.AuthenticationResponse
import org.ktugrades.common.Credentials
import org.ktugrades.common.ErrorMessage
import kotlinx.coroutines.*
import kotlinx.css.*
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.RequestInit
import react.*
import styled.*
import kotlin.browser.window

interface LoginPageState: RState {
    var username: String
    var password: String
    var errorMessage: String?
}

class LoginPage: RComponent<RProps, LoginPageState>() {

    override fun LoginPageState.init() {
        username = ""
        password = ""
    }

    private fun authenticateUser() = GlobalScope.launch {
        window.fetch("${SERVER_URL}/authenticate", RequestInit(
            method = "POST",
            headers = applicationJsonHeaders,
            body = JSON.stringify(Credentials(username = state.username, password = state.password))
        )).await().let {
            if (it.ok) {
                val encrypted = it.json().await().unsafeCast<AuthenticationResponse>()
                console.log(encrypted)
            } else {
                val error = it.json().await().unsafeCast<ErrorMessage>()
                setState {
                    errorMessage = error.message
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

fun RBuilder.loginPage(): ReactElement = child(LoginPage::class) {}

