package org.ktugrades.backend.models

/**
 * Created by simonas on 9/3/17.
 */

data class LoginModel(
        val studentName: String,
        val studentId: String,
        val currentWeek: String,
        val studentSemesters: List<YearModel>
)