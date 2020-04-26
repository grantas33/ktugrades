import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.list
import org.ktugrades.common.*
import org.w3c.fetch.RequestInit
import react.*
import react.dom.h1
import react.dom.tbody
import react.dom.thead
import styled.styledTable
import styled.styledTd
import styled.styledTh
import styled.styledTr
import kotlin.browser.window

interface GradesProps: RProps, LocalStorageProps

interface GradesState: RState {
    var markState: MarkState
}

class Grades: RComponent<GradesProps, GradesState>() {

    override fun GradesState.init() {
        markState = MarkState.Loading

        MainScope().launch {
            window.fetch("${SERVER_URL}/grades?username=${json.stringify(EncryptedUsername.serializer(), EncryptedUsername(username = getUsername()!!))}", RequestInit(
                method = "GET",
                headers = applicationJsonHeaders
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
        flexBox {
            when (state.markState) {
                is MarkState.Success -> styledTable {
                    thead {
                        styledTr {
                            styledTh { +"Mark" }
                            styledTh { +"Module" }
                            styledTh { +"Type" }
                            styledTh { +"Date" }
                        }
                    }
                    tbody {
                        (state.markState as MarkState.Success).marks.map {
                            styledTr {
                                styledTd { markComponent(it.marks) }
                                styledTd { moduleComponent(Module(it.moduleCode, it.semesterCode, it.title, it.professor)) }
                                styledTd { typeComponent(MarkType(it.typeId, it.week)) }
                                styledTd { +it.date.getFormatted() }
                            }
                        }
                    }
                }
                is MarkState.Error -> h1 {
                    +(state.markState as MarkState.Error).error.message
                }
                is MarkState.Loading -> loadingComponent()
            }
        }
    }
}

fun RBuilder.grades(handler: GradesProps.() -> Unit): ReactElement {
    return child(Grades::class) {
        this.attrs(handler)
    }
}

