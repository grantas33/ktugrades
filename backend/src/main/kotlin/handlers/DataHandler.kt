package org.ktugrades.backend.handlers

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.ktugrades.backend.models.MarkModel
import org.ktugrades.backend.models.ModuleModel
import org.ktugrades.backend.models.YearModel

/**
 * Created by simonas on 9/3/17.
 */

class DataHandler {

    fun getGrades(studCookie: String, planYear: YearModel)
            = getGrades(studCookie, planYear.year, planYear.id)

    fun getGrades(studCookie: String, planYear: String, studId: String): List<MarkModel> {
        val markList = mutableListOf<MarkModel>()
        val moduleList = getModules(studCookie, planYear, studId)

        moduleList.forEach { moduleModel ->
            val moduleMarkList = getModuleMarkList(moduleModel, studCookie)
            markList.addAll(moduleMarkList)
        }

        return markList
    }

    fun getModules(studCookie: String, planYear: String, studId: String): List<ModuleModel> {
        val url = "https://uais.cr.ktu.lt/ktuis/STUD_SS2.planas_busenos?" +
                "plano_metai=$planYear&" +
                "p_stud_id=$studId"

        val request = Jsoup.connect(url)
                .cookie("STUDCOOKIE", studCookie)
                .method(Connection.Method.GET)
                .execute()

        request.charset("windows-1257")
        val parse = request.parse()

        val moduleList = mutableListOf<ModuleModel>()

        val moduleTable = parse.select(".table.table-hover > tbody > tr")
        if (moduleTable.size > 0) {
            moduleTable.forEach { moduleElement ->
                val model = ModuleModel(moduleElement)
                moduleList.add(model)
            }
        }

        return moduleList
    }

    private fun getModuleMarkList(moduleModel: ModuleModel, studCookie: String): List<MarkModel> {
        val markList = mutableListOf<MarkModel>()
        val url = "https://uais.cr.ktu.lt/ktuis/STUD_SS2.infivert"

        val request = Jsoup.connect(url)
                .cookie("STUDCOOKIE", studCookie)
                .method(Connection.Method.POST)
                .data(mapOf(
                        "p1" to moduleModel.p1,
                        "p2" to moduleModel.p2,
                        "p3" to moduleModel.p3
                ))
                .execute()

        request.charset("windows-1257")
        val parse = request.parse()

        val markTable = parse.select(".d_grd2[style=\"border-collapse:collapse; empty-cells:hide;\"]").firstOrNull()
        val markInfoTable = parse.select(".d_grd2[style=\"border-collapse:collapse; table-layout:fixed; width:450px;\"]").firstOrNull()
        val headerInfo = parse.select("blockquote").select("p")

        if (markTable != null && markInfoTable != null) {
            val markTypeIdList: List<String> = getMarkTypeIdList(markTable)
            val infoTypeRowList: Map<String, String> = getMarkTypeMap(markInfoTable)
            val markWeekList: List<String> = getMarkWeekList(markTable)
            val markDataList: Map<Int, List<String>> = getMarkDataMap(markTable)
            val professorText = headerInfo[2].text()

            (0 until markWeekList.size-1).forEach { index ->
                if (markTypeIdList[index] != " ") {
                    val markModel = MarkModel(
                            name = moduleModel.module_name,
                            id = moduleModel.module_code,
                            semester = moduleModel.semester,
                            module_code = moduleModel.module_code,
                            module_name = moduleModel.module_name,
                            semester_number = moduleModel.semester_number,
                            credits = moduleModel.credits,
                            language = moduleModel.language,
                            professor = professorText,
                            typeId = markTypeIdList[index],
                            type = infoTypeRowList[markTypeIdList[index]],
                            week = markWeekList[index],
                            mark = markDataList[index] ?: listOf()
                    )

                    markList.add(markModel)
                }
            }
        }
        return markList
    }

    private fun getMarkTypeMap(element: Element): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            element.select("tr.dtr").forEach { typeElement ->
                val key = typeElement.children()[0].text()
                val value = typeElement.children()[1].text()
                put(key, value)
            }
        }
    }

    private fun getMarkWeekList(element: Element): MutableList<String> {
        val rowList = element.select("tr")
        return mutableListOf<String>().apply {
            val headerWeekRow = rowList[0].children()
                    .subList(4, rowList[0].children().size - 2)
            headerWeekRow.forEach { cell ->
                add(cell.text())
            }
        }
    }

    private fun getMarkTypeIdList(element: Element): MutableList<String> {
        val rowList = element.select("tr")
        return mutableListOf<String>().apply {
            val headerTypeRow = rowList[1].children()
                    .subList(1, rowList[1].children().size - 3)
            headerTypeRow.forEach { cell ->
                add(cell.text())
            }
        }
    }

    private fun getMarkDataMap(element: Element): Map<Int, List<String>> {
        val rowList = element.select("tr")
        return mutableMapOf<Int, MutableList<String>>().apply {
            (2 until rowList.size).forEach { rowIndex ->
                var headerDataRow = rowList[rowIndex].children().toList()
                if (rowIndex == 2) {
                    headerDataRow = headerDataRow.subList(4, rowList[rowIndex].children().size - 4)
                } else {
                    headerDataRow = headerDataRow.subList(1, rowList[rowIndex].children().size - 1)
                }
                headerDataRow.forEachIndexed { index, cell ->
                    val text = cell.text()
                    if (text != " ") {
                        if (containsKey(index)) {
                            put(index, get(index)!!.apply { add(text) })
                        } else {
                            put(index, mutableListOf(text))
                        }
                    }
                }
            }
        }
    }

}