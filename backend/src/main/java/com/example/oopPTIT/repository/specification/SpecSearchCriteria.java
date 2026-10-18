package com.example.oopPTIT.repository.specification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SpecSearchCriteria {
    private final String key;
    private final SearchOperation operation;
    private final Object value;
}
