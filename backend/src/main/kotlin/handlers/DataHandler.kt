package org.ktugrades.backend.handlers

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.Parameters
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.ktugrades.backend.getClient
import org.ktugrades.backend.models.MarkModel
import org.ktugrades.backend.models.ModuleModel

class DataHandler {

    suspend fun getGrades(planYear: String, studId: String): List<MarkModel> =
        getModules(planYear, studId).map {
            getModuleMarkList(it)
        }.flatten()

    suspend fun getModules(planYear: String, studId: String): List<ModuleModel> {
        val url = "https://uais.cr.ktu.lt/ktuis/STUD_SS2.planas_busenos?" +
                "plano_metai=$planYear&" +
                "p_stud_id=$studId"

        val parsed = getClient().get<HttpResponse> {
            url(url)
        }.let {
            Jsoup.parse(it.readText())
        }

        val moduleTable = parsed.select(".table.table-hover > tbody > tr")
        return moduleTable.map { moduleElement ->
            ModuleModel(moduleElement)
        }
    }

    private suspend fun getModuleMarkList(moduleModel: ModuleModel): List<MarkModel> {
        val markList = mutableListOf<MarkModel>()
        val url = "https://uais.cr.ktu.lt/ktuis/STUD_SS2.infivert"

        val parsed = getClient().post<HttpResponse> {
            url(url)
            body = FormDataContent(Parameters.build {
                append("p1", requireNotNull(moduleModel.p1))
                append("p2", requireNotNull(moduleModel.p2))
                append("p3", requireNotNull(moduleModel.p3))
            })
        }.let {
            Jsoup.parse(it.readText())
        }

        val markTable = parsed.select(".d_grd2[style=\"border-collapse:collapse; empty-cells:hide;\"]").firstOrNull()
        val markInfoTable = parsed.select(".d_grd2[style=\"border-collapse:collapse; table-layout:fixed; width:450px;\"]").firstOrNull()
        val headerInfo = parsed.select("blockquote").select("p")

        if (markTable != null && markInfoTable != null) {
            val markTypeIdList: List<String> = getMarkTypeIdList(markTable)
            val infoTypeRowList: Map<String, String> = getMarkTypeMap(markInfoTable)
            val markWeekList: List<String> = getMarkWeekList(markTable)
            val markDataList: Map<Int, List<String>> = getMarkDataMap(markTable)
            val professorText = headerInfo[2].text()

            (0 until markWeekList.size-1).forEach { index ->
                if (markTypeIdList[index] != " ") {
                    val markModel = MarkModel(
                            name = moduleModel.moduleName,
                            id = moduleModel.moduleCode,
                            semester = moduleModel.semester,
                            moduleCode = moduleModel.moduleCode,
                            moduleName = moduleModel.moduleName,
                            semesterNumber = moduleModel.semesterNumber,
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