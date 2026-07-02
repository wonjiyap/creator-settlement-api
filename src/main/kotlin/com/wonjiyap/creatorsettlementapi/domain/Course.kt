package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Transient

@Entity
class Course(
    @Id
    val id: String,
    val creatorId: String,
    val title: String,

    @Transient
    var creator: Creator? = null,
) {
    fun fromCreator(map: Map<String, Creator>) {
        map[this.creatorId]?.let { creator = it }
    }
}

fun List<Course>.fromCreators(list: List<Creator>) {
    val map = list.associateBy { it.id }
    this.forEach { it.fromCreator(map) }
}