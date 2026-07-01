package com.wonjiyap.creatorsettlementapi.repository

import com.wonjiyap.creatorsettlementapi.domain.Course
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, String>
