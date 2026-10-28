package com.example.oopPTIT.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateSubjectRequest;
import com.example.oopPTIT.dto.request.UpdateSubjectRequest;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/subjects")
@Validated
@RequiredArgsConstructor
public class AdminSubjectController {
    private final SubjectService subjectService;
    @PostMapping
    public ResponseData<Long> addSubject(@RequestBody @Valid CreateSubjectRequest request){
        Long subjectId = subjectService.addSubject(request);
        return new ResponseData<>(HttpStatus.CREATED.value(),"added subject",subjectId);
    }
    @PatchMapping("/{id}")
    public ResponseData<?> updateSubject(@PathVariable Long id, @RequestBody @Valid UpdateSubjectRequest request){
        subjectService.updatedSubject(id,request);
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Updated subject");
    }
    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return new ResponseData<>(HttpStatus.OK.value(), "Subject deleted successfully");
    }
    @GetMapping
    public ResponseData<ResponsePage<?>> getSubjectWithSpecification(
            Pageable pageable,
            @RequestParam(required = false) String[] subject,
            @RequestParam(required = false) String search) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get all subject",
                subjectService.advanceSearchWithSpecification(pageable, subject, search));
    }
}
