package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Course(
    @Id
    val id: String,
    val creatorId: String,
    val title: String,
)
