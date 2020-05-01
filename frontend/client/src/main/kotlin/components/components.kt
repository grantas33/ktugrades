package components

import MarkType
import kotlinx.css.*
import kotlinx.html.BUTTON
import kotlinx.html.DIV
import org.ktugrades.common.Module
import org.ktugrades.common.toMarkString
import react.RBuilder
import react.ReactElement
import react.dom.RDOMBuilder
import styled.StyledDOMBuilder
import styled.css
import styled.styledButton
import styled.styledDiv

fun RBuilder.flexBox(block: RDOMBuilder<DIV>.() -> Unit) = styledDiv {
    css {
        display = Display.flex
        justifyContent = JustifyContent.center
        alignItems = Align.center
        minWidth = LinearDimension("100%")
        minHeight = LinearDimension("100%")
        flexDirection = FlexDirection.column
    }
    block()
}

fun RBuilder.appButton(block: StyledDOMBuilder<BUTTON>.() -> Unit) = styledButton {
    css {
        fontSize = LinearDimension("16px")
        padding = "8px 20px 8px 19px"
        border = "2px solid #000101"
        borderRadius = LinearDimension("3px")
        background = "transparent"
        cursor = Cursor.pointer
        fontFamily = "pf-dintext-promedium"
    }
    block()
}

fun RBuilder.loadingComponent() = LoadingSpinner {
    attrs {
        type="MutatingDots"
        color="green"
        height=100
        width=100
    }
}

fun RBuilder.markComponent(marks: List<String>): ReactElement {
    val markString = marks.toMarkString()
    return styledDiv {
        css {
            fontSize = LinearDimension("4rem")
        }
        + markString
    }
}

fun RBuilder.moduleComponent(module: Module) = styledDiv {
    css {
        display = Display.flex
        flexDirection = FlexDirection.column
    }
    styledDiv {
        css {
            fontSize = LinearDimension("2.5rem")
            mobileView {
                fontSize = LinearDimension("1.5rem")
                textAlign = TextAlign.center
            }
        }
        +module.title
    }
    styledDiv {
        css {
            display = Display.flex
            justifyContent = JustifyContent.spaceBetween
        }
        styledDiv { +module.code }
        styledDiv {
            css {
                textAlign = TextAlign.right
                marginLeft = LinearDimension("5px")
            }
            +module.professor
        }
    }
}

fun RBuilder.typeComponent(type: MarkType) = styledDiv {
    css {
        display = Display.flex
        flexDirection = FlexDirection.column
    }
    styledDiv {
        css {
            fontSize = LinearDimension("2.5rem")
        }
        +(type.typeId ?: "-")
    }
    styledDiv {
        css {
            whiteSpace = WhiteSpace.nowrap
        }
        +"${if (type.weeks.split("-").size > 1) "Weeks" else "Week"} ${type.weeks}"
    }
}

fun CSSBuilder.mobileView(block: RuleSet) {
    media("(max-width: 768px)") {
        block()
    }
}



