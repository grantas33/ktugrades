@file:JsModule("react-loader-spinner")
@file:JsNonModule

package components

import react.*

@JsName("default")
external val LoadingSpinner: RClass<LoadingSpinnerProps>

external interface LoadingSpinnerProps : RProps {
    var type: String
    var color: String
    var height: Int
    var width: Int
    var timeout: Int
}