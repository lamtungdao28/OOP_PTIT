package com.example.oopPTIT.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** POST /api/sessions/{id}/attendance/recognize, multipart/form-data. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecognizeAttendanceRequest {

    @NotNull
    private MultipartFile image;

    @JsonIgnore
    @AssertTrue(message = "image must not be empty")
    public boolean isImageNotEmpty() {
        return image == null || !image.isEmpty();
    }
}
