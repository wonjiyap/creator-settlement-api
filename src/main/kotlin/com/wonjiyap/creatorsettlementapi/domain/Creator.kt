package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Creator(
    @Id
    val id: String,
    val name: String,
)
