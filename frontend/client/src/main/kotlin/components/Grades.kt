package components

import LocalStorageProps
import MarkState
import MarkType
import SERVER_URL
import getUsername
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import json
import jsonClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.ktugrades.common.*
import react.*
import react.dom.h1
import react.dom.tbody
import react.dom.thead
import styled.styledTable
import styled.styledTd
import styled.styledTh
import styled.styledTr

interface GradesProps: RProps, LocalStorageProps

interface GradesState: RState {
    var markState: MarkState
}

class Grades: RComponent<GradesProps, GradesState>() {

    override fun GradesState.init() {
        markState = MarkState.Loading

        val serializedUsername = json.stringify(EncryptedUsername.serializer(), EncryptedUsername(username = getUsername()!!))
        MainScope().launch {
            jsonClient.get<HttpResponse>("${SERVER_URL}${Routes.Grades}?username=${serializedUsername}").let {
                when {
                    it.status.isSuccess() -> {
                        val marks = it.receive<List<MarkInfoResponse>>()
                        setState {
                            markState = MarkState.Success(marks = marks)
                        }
                    }
                    else -> {
                        val error = it.receive<ErrorMessage>()
                        setState {
                            markState = MarkState.Error(error = error)
                        }
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

