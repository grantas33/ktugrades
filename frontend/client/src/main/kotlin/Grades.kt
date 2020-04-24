import react.*
import react.dom.h1

interface GradesProps: RProps, LocalStorageProps


class Grades: RComponent<GradesProps, RState>() {


    override fun RBuilder.render() {
        h1 {
            +"Grades"
        }
    }
}

fun RBuilder.grades(handler: GradesProps.() -> Unit): ReactElement {
    return child(Grades::class) {
        this.attrs(handler)
    }
}

