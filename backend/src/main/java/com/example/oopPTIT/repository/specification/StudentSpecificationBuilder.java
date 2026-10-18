package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.exception.InvalidStudentFilterException;
import com.example.oopPTIT.util.Status;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.oopPTIT.repository.specification.SearchOperation.*;

public class StudentSpecificationBuilder {
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+?)([<:>~!])(.*)");
    private final List<SpecSearchCriteria> params = new ArrayList<>();

    public StudentSpecificationBuilder with(String filter) {
        if (filter == null || filter.isBlank()) {
            return this;
        }

        Matcher matcher = FILTER_PATTERN.matcher(filter.strip());
        if (!matcher.matches()) {
            throw new InvalidStudentFilterException("Invalid student filter; expected field followed by :, !, >, < or ~ and a value");
        }

        String key = matcher.group(1);
        SearchOperation operation = SearchOperation.getSimpleOperation(matcher.group(2).charAt(0));
        String value = matcher.group(3).strip();
        if (operation == EQUALITY && !value.isEmpty()) {
            boolean startsWithWildcard = value.startsWith("*");
            boolean endsWithWildcard = value.endsWith("*");
            if (startsWithWildcard || endsWithWildcard) {
                operation = startsWithWildcard && endsWithWildcard ? CONTAINS
                        : startsWithWildcard ? ENDS_WITH : STARTS_WITH;
                if (startsWithWildcard) {
                    value = value.substring(1);
                }
                if (endsWithWildcard && !value.isEmpty()) {
                    value = value.substring(0, value.length() - 1);
                }
                value = value.strip();
            }
        }
        if (value.isEmpty() || value.length() > 255) {
            throw new InvalidStudentFilterException("Student filter value must contain 1 to 255 characters");
        }

        Object typedValue = parseValue(key, value);
        if (!(typedValue instanceof String) && (operation == LIKE || operation == CONTAINS
                || operation == STARTS_WITH || operation == ENDS_WITH)) {
            throw new InvalidStudentFilterException("Text matching is not supported for student field: " + key);
        }
        if (typedValue instanceof Status && operation != EQUALITY && operation != NEGATION) {
            throw new InvalidStudentFilterException("Student status only supports : and !");
        }

        params.add(new SpecSearchCriteria(key, operation, typedValue));
        return this;
    }

    public Specification<Student> build() {
        Specification<Student> specification = Specification.unrestricted();
        for (SpecSearchCriteria criteria : params) {
            specification = specification.and(new StudentSpecification(criteria));
        }
        return specification;
    }

    private Object parseValue(String key, String value) {
        try {
            return switch (key) {
                case "fullName", "studentCode", "phone", "username", "email" -> value;
                case "id" -> Long.valueOf(value);
                case "dateOfBirth" -> LocalDate.parse(value);
                case "status" -> Status.valueOf(value.toUpperCase(Locale.ROOT));
                default -> throw new InvalidStudentFilterException("Unsupported student filter field: " + key);
            };
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new InvalidStudentFilterException("Invalid value for student filter field: " + key);
        }
    }
}
