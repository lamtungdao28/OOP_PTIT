package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.CreateSubjectRequest;
import com.example.oopPTIT.dto.request.UpdateSubjectRequest;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponseError;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/subjects")
@Validated
@RequiredArgsConstructor
public class AdminSubjectController {
    private final SubjectService subjectService;
    @PostMapping
    public ResponseData<Long> addSubject(@RequestBody @Valid CreateSubjectRequest request){
        try{
            Long subjectId = subjectService.addSubject(request);
            return new ResponseData<>(HttpStatus.CREATED.value(),"added subject",subjectId);
        }catch (Exception e){
            return new ResponseError(HttpStatus.BAD_REQUEST.value(),"error to add new subject");
        }
    }
    @PatchMapping("/{id}")
    public ResponseData<?> updateSubject(@PathVariable Long id, @RequestBody @Valid UpdateSubjectRequest request){
        try{
            subjectService.updatedSubject(id,request);
            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Updated subject");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), "Can't find subject");
        }

    }
    @GetMapping
    public ResponseData<ResponsePage<?>> getSubjectWithSpecification(Pageable pageable, @RequestParam(required = false) String[] subject){
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get all subject", subjectService.advanceSearchWithSpecification(pageable,subject));
    }
}
