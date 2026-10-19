package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.util.Status;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RequiredArgsConstructor
public class CourseClassSpecification implements Specification<CourseClass> {
    private final SpecSearchCriteria criteria;

    @Override
    public @Nullable Predicate toPredicate(Root<CourseClass> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        String value = criteria.getValue().toString();
        try {
            return switch (criteria.getKey()) {
                case "id" -> buildPredicate(root.get("id"), Long.valueOf(value), builder);
                case "classCode" -> buildPredicate(root.get("classCode"), value, builder);
                case "maxStudents" -> buildPredicate(root.get("maxStudents"), Integer.valueOf(value), builder);
                case "status" -> buildPredicate(root.get("status"), Status.valueOf(value), builder);
                case "subjectId" -> buildPredicate(root.get("subject").get("id"), Long.valueOf(value), builder);
                case "semesterId" -> buildPredicate(root.get("semester").get("id"), Long.valueOf(value), builder);
                case "lecturerId" -> buildPredicate(root.get("lecturer").get("id"), Long.valueOf(value), builder);
                case "createdAt", "updatedAt" -> buildPredicate(root.get(criteria.getKey()),
                        LocalDateTime.parse(value), builder);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown course class filter: " + criteria.getKey());
            };
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid value for course class filter: " + criteria.getKey(), e);
        }
    }

    private <T extends Comparable<? super T>> Predicate buildPredicate(Path<T> path, T value, CriteriaBuilder builder) {
        if (value instanceof Status && criteria.getOperation() != SearchOperation.EQUALITY
                && criteria.getOperation() != SearchOperation.NEGATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status only supports : and ! filters");
        }
        return switch (criteria.getOperation()) {
            case EQUALITY -> builder.equal(path, value);
            case NEGATION -> builder.notEqual(path, value);
            case GREATER_THAN -> builder.greaterThan(path, value);
            case LESS_THAN -> builder.lessThan(path, value);
            case LIKE, CONTAINS -> like(path, value, builder, "%", "%");
            case STARTS_WITH -> like(path, value, builder, "", "%");
            case ENDS_WITH -> like(path, value, builder, "%", "");
        };
    }

    private Predicate like(Path<?> path, Object value, CriteriaBuilder builder, String prefix, String suffix) {
        if (!(value instanceof String)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text filters require a string field");
        }
        return builder.like(path.as(String.class), prefix + value + suffix);
    }
}
