package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.StudentHasCourseClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class StudentInCourseClassSpecification implements Specification<Student> {
    private final Long courseClassId;

    @Override
    public Predicate toPredicate(Root<Student> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        // A subquery prevents repeated enrollments from duplicating students or page counts.
        Subquery<Long> enrolledStudents = query.subquery(Long.class);
        Root<StudentHasCourseClass> enrollment = enrolledStudents.from(StudentHasCourseClass.class);
        enrolledStudents.select(enrollment.get("student").get("id"))
                .where(builder.equal(enrollment.get("courseClass").get("id"), courseClassId));
        return root.get("id").in(enrolledStudents);
    }
}
