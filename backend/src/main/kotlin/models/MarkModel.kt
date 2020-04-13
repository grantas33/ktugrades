package org.ktugrades.backend.models

/**
 * Created by simonas on 9/16/17.
 */

class MarkModel(
    val name: String,
    val id: String,
    val semester: String,
    val moduleCode: String,
    val moduleName: String,
    val semesterNumber: String,
    val credits: String,
    val language: String,
    val professor: String,
    val typeId: String,
    val type: String?,
    val week: String,
    val mark: List<String>
)