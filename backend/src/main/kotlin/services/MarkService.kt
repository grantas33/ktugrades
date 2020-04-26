package org.ktugrades.backend.services

import org.joda.time.DateTime
import org.ktugrades.backend.MarkAggregationResult
import org.ktugrades.backend.MarkInfo
import org.ktugrades.backend.Module
import org.ktugrades.backend.handlers.DataHandler
import org.ktugrades.backend.sort
import java.lang.RuntimeException
import java.util.*

class MarkService(private val dataHandler: DataHandler, private val mySqlProvider: MySqlProvider) {

    suspend fun getMarks(username: ByteArray): MarkAggregationResult {
        dataHandler.setEnglishLanguageForClient()
        val realtimeMarks = dataHandler.getInfo().let {
            it.studentSemesters.sortedByDescending { it.year }.take(2).map { yearModel ->
                dataHandler.getGrades(planYear = yearModel.year, studId = yearModel.id).map {
                    MarkInfo(
                        id = UUID.randomUUID(),
                        moduleCode = it.moduleCode,
                        semesterCode = getSemesterCode(year = yearModel.year, semester = it.semester),
                        title = it.moduleName,
                        professor = it.professor,
                        typeId = it.typeId,
                        week = it.week,
                        date = DateTime.now(),
                        marks = it.mark.filter { it.isNotBlank() }
                    )
                }
            }.flatten().filter { it.marks.any { it.isNotBlank() } }
        }
        val databaseMarks = mySqlProvider.getMarksForUser(username)

        val markInfoToAddAndNotify = mutableListOf<MarkInfo>()
        val markInfoToUpdateAndNotify = mutableListOf<MarkInfo>()
        val markInfoToUpdate = mutableListOf<MarkInfo>()

        realtimeMarks.forEach { curr ->
            val replica = databaseMarks.find { curr.moduleCode == it.moduleCode && curr.semesterCode == it.semesterCode && curr.week == it.week && curr.typeId == it.typeId }
            when {
                replica == null -> markInfoToAddAndNotify.add(curr)
                replica.marks.containsAll(curr.marks).not() -> markInfoToUpdateAndNotify.add(
                    replica.copy(typeId = curr.typeId, date = DateTime.now(), marks = curr.marks)
                )
                curr.marks.containsAll(replica.marks).not()  ->
                    markInfoToUpdate.add(replica.copy(typeId = curr.typeId, marks = curr.marks))
            }
        }

        val newModules = markInfoToAddAndNotify
            .filter { newMark -> databaseMarks.any { it.moduleCode == newMark.moduleCode && it.semesterCode == newMark.semesterCode }.not() }
            .map {
                Module(
                    code = it.moduleCode,
                    semesterCode = it.semesterCode,
                    title = it.title,
                    professor = it.professor
                )
            }
            .distinct()

        val updatedMarks = (markInfoToAddAndNotify + markInfoToUpdateAndNotify + markInfoToUpdate)
        val latestMarks = (databaseMarks.filter { dbMark -> updatedMarks.any { dbMark.id == it.id }.not() } + updatedMarks).sort().take(50)

        return MarkAggregationResult(
            markInfoToAddAndNotify = markInfoToAddAndNotify,
            markInfoToUpdateAndNotify = markInfoToUpdateAndNotify,
            markInfoToUpdate = markInfoToUpdate,
            latestMarks = latestMarks,
            newModules = newModules
        )
    }

    private fun getSemesterCode(year: String, semester: String): String {
        val season = when(semester) {
            "Autumn semester" -> "A"
            "Spring semester" -> "S"
            else -> throw RuntimeException("Unhandled semester: $semester")
        }
        return "${year}${season}"
    }
}