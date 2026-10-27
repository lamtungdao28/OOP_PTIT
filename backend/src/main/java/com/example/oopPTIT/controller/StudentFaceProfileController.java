package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.response.FaceProfileResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.service.FaceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students/me/face-profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentFaceProfileController {
    private final FaceProfileService faceProfileService;

    @GetMapping
    public ResponseData<FaceProfileResponse> getMyProfile() {
        return new ResponseData<>(HttpStatus.OK.value(), "Face profile status retrieved",
                faceProfileService.getMyProfile());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<FaceProfileResponse>> register(@RequestPart("image") MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(HttpStatus.CREATED.value(), "Face profile registered",
                        faceProfileService.register(image)));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseData<FaceProfileResponse> replace(@RequestPart("image") MultipartFile image) {
        return new ResponseData<>(HttpStatus.OK.value(), "Face profile replaced",
                faceProfileService.replace(image));
    }
}
