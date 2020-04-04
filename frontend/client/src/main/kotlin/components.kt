import kotlinx.css.*
import kotlinx.html.BUTTON
import kotlinx.html.DIV
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.h1
import styled.css
import styled.styledButton
import styled.styledDiv

fun RBuilder.flexBox(block: RDOMBuilder<DIV>.() -> Unit) = styledDiv {
    css {
        display = Display.flex
        justifyContent = JustifyContent.center
        alignItems = Align.center
        width = LinearDimension("100%")
        height = LinearDimension("100%")
        flexDirection = FlexDirection.column
    }
    block()
}

fun RBuilder.appButton(block: RDOMBuilder<BUTTON>.() -> Unit) = styledButton {
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

fun RBuilder.loadingComponent() = h1 {
    +"Loading"
}

