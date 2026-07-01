package com.wonjiyap.creatorsettlementapi.repository

import com.wonjiyap.creatorsettlementapi.domain.CancelRecord
import org.springframework.data.jpa.repository.JpaRepository

interface CancelRecordRepository : JpaRepository<CancelRecord, String>
