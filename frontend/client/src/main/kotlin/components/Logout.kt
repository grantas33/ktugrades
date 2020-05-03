package components

import CacheProps
import kotlinx.css.*
import react.*
import styled.*

interface LogoutProps: RProps, CacheProps

val Logout = functionalComponent<LogoutProps> {
    appButton {
        css {
            position = Position.absolute
            top = LinearDimension("10px")
            right = LinearDimension("12%")
            mobileView {
                right = LinearDimension("10px")
            }
        }
        +"Logout"
    }
}
