package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Transient
import java.time.LocalDateTime
import kotlin.collections.forEach

@Entity
class SaleRecord(
    @Id
    val id: String,
    val courseId: String,
    val studentId: String,
    val amount: Long,
    val paidAt: LocalDateTime,

    @Transient
    var course: Course? = null,
) {
    fun fromCourse(map: Map<String, Course>) {
        map[this.courseId]?.let { course = it }
    }
}

fun List<SaleRecord>.fromCourses(list: List<Course>) {
    val map = list.associateBy { it.id }
    this.forEach { it.fromCourse(map) }
}