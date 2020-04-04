package org.ktugrades.backend.models

import org.jsoup.nodes.Element

/**
 * Created by simonas on 9/16/17.
 */

class ModuleModel(
        val semester: String,
        val semester_number: String,
        val module_code: String,
        val module_name: String,
        val credits: String,
        val language: String,
        val misc: String,
        val p1: String?,
        val p2: String?,
        val p3: String?
) {
    constructor(element: Element): this(
            semester = element.getSemester(),
            semester_number = element.getSemesterNumber(),
            module_code = element.getModuleCode(),
            module_name = element.getModuleName(),
            credits = element.getCredits(),
            language = element.getLanguage(),
            misc = element.getMisc(),
            p1 = element.getP1(),
            p2 = element.getP2(),
            p3 = element.getP3()
    )

}

private fun Element.getSemester()
        = parent().parent().children().first().children().first().text().split("(")[0].trim()

private fun Element.getSemesterNumber()
        = parent().parent().children().first().children().first().text().split("(")[1].split(')')[0].trim()

private fun Element.getModuleCode()
        = children().first().text()

private fun Element.getModuleName()
        = children().eq(1).text()

private fun Element.getCredits()
        = children().eq(3).text()

private fun Element.getLanguage()
        = children().eq(4).text()

private fun Element.getMisc()
        = children().eq(5).text()

private fun Element.getP1(): String? = getInfivertFunctionParams()?.get(0)

private fun Element.getP2(): String? = getInfivertFunctionParams()?.get(1)

private fun Element.getP3(): String? = getInfivertFunctionParams()?.get(2)

private fun Element.getInfivertFunctionParams(): List<String>? =
    children().getOrNull(5)?.children()?.first()?.attr("onclick")
        ?.substringAfter("(")
        ?.substringBeforeLast(")")
        ?.filter { " +\'".contains(it).not() }
        ?.split(",")