package com.example.oopPTIT.repository.specification;

import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.exception.InvalidLecturerFilterException;
import com.example.oopPTIT.util.Status;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.oopPTIT.repository.specification.SearchOperation.*;

public class LecturerSpecificationBuilder {
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+?)([<:>~!])(.*)");
    private final List<SpecSearchCriteria> params = new ArrayList<>();

    public LecturerSpecificationBuilder with(String filter) {
        if (filter == null || filter.isBlank()) {
            return this;
        }

        Matcher matcher = FILTER_PATTERN.matcher(filter.strip());
        if (!matcher.matches()) {
            throw new InvalidLecturerFilterException("Invalid lecturer filter; expected field followed by :, !, >, < or ~ and a value");
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
            throw new InvalidLecturerFilterException("Lecturer filter value must contain 1 to 255 characters");
        }

        Object typedValue = parseValue(key, value);
        if (!(typedValue instanceof String) && (operation == LIKE || operation == CONTAINS
                || operation == STARTS_WITH || operation == ENDS_WITH)) {
            throw new InvalidLecturerFilterException("Text matching is not supported for lecturer field: " + key);
        }
        if (typedValue instanceof Status && operation != EQUALITY && operation != NEGATION) {
            throw new InvalidLecturerFilterException("Lecturer status only supports : and !");
        }

        params.add(new SpecSearchCriteria(key, operation, typedValue));
        return this;
    }

    public Specification<Lecturer> build() {
        Specification<Lecturer> specification = Specification.unrestricted();
        for (SpecSearchCriteria criteria : params) {
            specification = specification.and(new LecturerSpecification(criteria));
        }
        return specification;
    }

    private Object parseValue(String key, String value) {
        try {
            return switch (key) {
                case "fullName", "lecturerCode", "department", "phone", "username", "email" -> value;
                case "id" -> Long.valueOf(value);
                case "status" -> Status.valueOf(value.toUpperCase(Locale.ROOT));
                default -> throw new InvalidLecturerFilterException("Unsupported lecturer filter field: " + key);
            };
        } catch (IllegalArgumentException exception) {
            throw new InvalidLecturerFilterException("Invalid value for lecturer filter field: " + key);
        }
    }
}
