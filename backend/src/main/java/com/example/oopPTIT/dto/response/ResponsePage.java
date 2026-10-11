package com.example.oopPTIT.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponsePage<T> {
    private int pageNo;
    private int pageSize;
    private int totalPage;
    private T items;

}
