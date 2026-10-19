package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.CourseClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static com.example.oopPTIT.repository.specification.SearchOperation.ZERO_OR_MORE_REGEX;

public class CourseClassSpecificationBuilder {
    public final List<SpecSearchCriteria> param;

    public CourseClassSpecificationBuilder() {
        this.param = new ArrayList<>();
    }

    public CourseClassSpecificationBuilder with(String key, String operation, String value, String prefix, String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public CourseClassSpecificationBuilder with(String orPredicate, String key, String operation,
                                                String value, String prefix, String suffix) {
        SearchOperation oper = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (oper == SearchOperation.EQUALITY) {
            boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
            boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);
            if (startWithAsterisk && endWithAsterisk) {
                oper = SearchOperation.CONTAINS;
            } else if (startWithAsterisk) {
                oper = SearchOperation.ENDS_WITH;
            } else if (endWithAsterisk) {
                oper = SearchOperation.STARTS_WITH;
            }
        }
        param.add(new SpecSearchCriteria(orPredicate, key, oper, value));
        return this;
    }

    public Specification<CourseClass> build() {
        if (param.isEmpty()) return null;

        Specification<CourseClass> specification = new CourseClassSpecification(param.get(0));
        for (int i = 1; i < param.size(); i++) {
            specification = param.get(i).getOrPredicate()
                    ? specification.or(new CourseClassSpecification(param.get(i)))
                    : specification.and(new CourseClassSpecification(param.get(i)));
        }
        return specification;
    }
}
