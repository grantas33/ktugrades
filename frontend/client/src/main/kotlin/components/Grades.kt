package components

import MarkState
import MarkTypeInfo
import SERVER_URL
import hooks.useMobileView
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import jsonClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import org.ktugrades.common.*
import react.*
import react.dom.h1
import react.dom.tbody
import react.dom.thead
import services.getUsername
import styled.*
import kotlin.browser.window

interface GradesProps: RProps

val Grades = functionalComponent<GradesProps> {

    val (markState, setMarkState) = useState(MarkState.Loading as MarkState)
    val isMobileView = useMobileView()

    useEffectWithCleanup (dependencies = listOf()) {
        val job = MainScope().launch {
            val serializedUsername = jsonSerializer.stringify(EncryptedUsername.serializer(), EncryptedUsername(username = window.caches.getUsername()!!))
            jsonClient.get<HttpResponse>("${SERVER_URL}${Routes.Grades}?username=${serializedUsername}").let {
                when {
                    it.status.isSuccess() -> {
                        val marks = it.receive<List<MarkInfoResponse>>()
                        setMarkState(MarkState.Success(marks = marks.sort()))
                    }
                    else -> {
                        val error = it.receive<ErrorMessage>()
                        setMarkState(MarkState.Error(error = error))
                    }
                }
            }
        }

        return@useEffectWithCleanup { job.cancel() }
    }

    when (markState) {
        is MarkState.Success -> {
            when {
                isMobileView -> styledDiv {
                    css {
                        position = Position.relative
                        top = LinearDimension("50px")
                    }
                    markState.marks.map {
                        styledDiv {
                            css {
                                display = Display.flex
                                flexDirection = FlexDirection.column
                                alignItems = Align.center
                                border = "2px solid #000101"
                                borderRadius = LinearDimension("3px")
                                minWidth = LinearDimension("80%")
                                margin(vertical = LinearDimension("5px"), horizontal = LinearDimension("10px"))
                                padding(vertical = LinearDimension("15px"), horizontal = LinearDimension("30px"))
                            }
                            markComponent(it.marks)
                            moduleComponent(Module(it.moduleCode, it.semesterCode, it.title, it.professor))
                            typeComponent(MarkTypeInfo(it.typeId, it.week, it.averageMarkForModule))
                            styledDiv {
                                css {
                                    textAlign = TextAlign.center
                                    marginTop = LinearDimension("10px")
                                }
                                +it.date.getFormatted()
                            }
                        }
                    }
                }
                else -> styledTable {
                    thead {
                        styledTr {
                            styledTh {
                                css { borderBottom = "2px solid #000101" }
                                +"Mark"
                            }
                            styledTh {
                                css { borderBottom = "2px solid #000101" }
                                +"Module"
                            }
                            styledTh {
                                css { borderBottom = "2px solid #000101" }
                                +"Type"
                            }
                            styledTh {
                                css { borderBottom = "2px solid #000101" }
                                +"Date"
                            }
                        }
                    }
                    tbody {
                        markState.marks.map {
                            styledTr {
                                styledTd { markComponent(it.marks) }
                                styledTd { moduleComponent(Module(it.moduleCode, it.semesterCode, it.title, it.professor)) }
                                styledTd { typeComponent(MarkTypeInfo(it.typeId, it.week, it.averageMarkForModule)) }
                                styledTd { +it.date.getFormatted() }
                            }
                        }
                    }
                }
            }
        }
        is MarkState.Error -> h1 {
            +markState.error.message
        }
        is MarkState.Loading -> loadingComponent()
    }
}
