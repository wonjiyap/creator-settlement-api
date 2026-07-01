package com.wonjiyap.creatorsettlementapi.repository

import com.wonjiyap.creatorsettlementapi.domain.Creator
import org.springframework.data.jpa.repository.JpaRepository

interface CreatorRepository : JpaRepository<Creator, String>
