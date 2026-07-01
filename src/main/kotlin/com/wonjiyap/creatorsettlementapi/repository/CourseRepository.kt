package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.Course
import com.wonjiyap.creatorsettlementapi.domain.QCourse.course
import com.wonjiyap.creatorsettlementapi.repository.dto.CourseFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CourseFetchParam
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, String>, CourseCustomRepository

interface CourseCustomRepository {
    fun fetch(param: CourseFetchParam): List<Course>
    fun fetchOne(param: CourseFetchOneParam): Course?
}

class CourseCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CourseCustomRepository {

    override fun fetch(param: CourseFetchParam): List<Course> =
        queryFactory
            .selectFrom(course)
            .where(
                param.creatorId?.let { course.creatorId.eq(it) },
                param.title?.let { course.title.containsIgnoreCase(it) },
            )
            .fetch()

    override fun fetchOne(param: CourseFetchOneParam): Course? =
        queryFactory
            .selectFrom(course)
            .where(
                param.id?.let { course.id.eq(it) },
                param.creatorId?.let { course.creatorId.eq(it) },
            )
            .limit(1)
            .fetchOne()
}
