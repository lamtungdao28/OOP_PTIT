package com.example.oopPTIT.repository.specification;

public enum SearchOperation {
    EQUALITY, NEGATION, GREATER_THAN, LESS_THAN, LIKE, STARTS_WITH, ENDS_WITH, CONTAINS;

    public static SearchOperation getSimpleOperation(char input) {
        return switch (input) {
            case ':' -> EQUALITY;
            case '!' -> NEGATION;
            case '>' -> GREATER_THAN;
            case '<' -> LESS_THAN;
            case '~' -> LIKE;
            default -> null;
        };
    }
}
