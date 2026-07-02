package com.wonjiyap.creatorsettlementapi.repository.dto

data class CreatorFetchOneParam(
    val id: String? = null,
)

data class CreatorFetchParam(
    val idArr: List<String>? = null,
)
