package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.StudentHasCourseClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class CourseClassForStudentSpecification implements Specification<CourseClass> {
    private final Long studentId;

    @Override
    public Predicate toPredicate(Root<CourseClass> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        // A subquery prevents duplicate enrollments from inflating results and page counts.
        Subquery<Long> enrolledClasses = query.subquery(Long.class);
        Root<StudentHasCourseClass> enrollment = enrolledClasses.from(StudentHasCourseClass.class);
        enrolledClasses.select(enrollment.get("courseClass").get("id"))
                .where(builder.equal(enrollment.get("student").get("id"), studentId));
        return root.get("id").in(enrolledClasses);
    }
}
