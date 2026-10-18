package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.Student;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

import static com.example.oopPTIT.repository.specification.SearchOperation.NEGATION;

@RequiredArgsConstructor
public class StudentSpecification implements Specification<Student> {
    private final SpecSearchCriteria criteria;

    @Override
    public Predicate toPredicate(Root<Student> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return switch (criteria.getKey()) {
            case "id" -> compare(root.get("id"), (Long) criteria.getValue(), builder);
            case "dateOfBirth" -> compare(root.get("dateOfBirth"), (LocalDate) criteria.getValue(), builder);
            case "status" -> criteria.getOperation() == NEGATION
                    ? builder.notEqual(root.get("user").get("status"), criteria.getValue())
                    : builder.equal(root.get("user").get("status"), criteria.getValue());
            default -> {
                Expression<String> field = switch (criteria.getKey()) {
                    case "username", "email" -> root.get("user").get(criteria.getKey());
                    default -> root.get(criteria.getKey());
                };
                yield matchText(builder.lower(field), criteria.getValue().toString().toLowerCase(Locale.ROOT), builder);
            }
        };
    }

    private Predicate matchText(Expression<String> field, String value, CriteriaBuilder builder) {
        // Only the documented * syntax is a wildcard; SQL LIKE metacharacters stay literal.
        String escaped = value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return switch (criteria.getOperation()) {
            case LIKE, CONTAINS -> builder.like(field, "%" + escaped + "%", '!');
            case STARTS_WITH -> builder.like(field, escaped + "%", '!');
            case ENDS_WITH -> builder.like(field, "%" + escaped, '!');
            default -> compare(field, value, builder);
        };
    }

    private <T extends Comparable<? super T>> Predicate compare(
            Expression<T> field, T value, CriteriaBuilder builder) {
        return switch (criteria.getOperation()) {
            case EQUALITY -> builder.equal(field, value);
            case NEGATION -> builder.notEqual(field, value);
            case GREATER_THAN -> builder.greaterThan(field, value);
            case LESS_THAN -> builder.lessThan(field, value);
            default -> throw new IllegalStateException("Unsupported comparison: " + criteria.getOperation());
        };
    }
}
