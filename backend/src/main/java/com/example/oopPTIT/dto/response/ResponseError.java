package com.example.oopPTIT.dto.response;

public class ResponseError extends ResponseData<Void>{
    public ResponseError(String status, String message) {
        super(status, message);
    }
}
