import kotlinx.css.*
import react.dom.render
import styled.StyledComponents
import styled.injectGlobal
import kotlin.browser.document

fun main() {
    val styles = CSSBuilder().apply {
        html {
            width = LinearDimension("100%")
            height = LinearDimension("100%")
            fontFamily = "pf-dintext-promedium"
        }
        body {
            margin = "0"
            padding = "0"
            width = LinearDimension("100%")
            height = LinearDimension("100%")
        }
        "#root" {
            width = LinearDimension("100%")
            height = LinearDimension("100%")
        }
    }

    StyledComponents.injectGlobal(styles.toString())

    render(document.getElementById("root")) {
        child(App::class) {}
    }
}
