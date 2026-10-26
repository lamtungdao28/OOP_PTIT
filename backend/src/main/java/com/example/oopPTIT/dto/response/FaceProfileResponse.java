package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.FaceProfile;
import com.example.oopPTIT.util.Status;

import java.time.LocalDateTime;

public record FaceProfileResponse(boolean registered, Status status, String modelName,
                                  LocalDateTime updatedAt) {
    public static FaceProfileResponse empty() {
        return new FaceProfileResponse(false, null, null, null);
    }

    public static FaceProfileResponse from(FaceProfile profile) {
        return new FaceProfileResponse(true, profile.getStatus(), profile.getModelName(),
                profile.getUpdatedAt());
    }
}
