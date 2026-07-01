package com.wonjiyap.creatorsettlementapi.repository

import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import org.springframework.data.jpa.repository.JpaRepository

interface SaleRecordRepository : JpaRepository<SaleRecord, String>
