package com.wonjiyap.creatorsettlementapi.repository.dto

data class CourseFetchParam(
    val creatorId: String? = null,
    val title: String? = null,
)

data class CourseFetchOneParam(
    val id: String? = null,
    val creatorId: String? = null,
)