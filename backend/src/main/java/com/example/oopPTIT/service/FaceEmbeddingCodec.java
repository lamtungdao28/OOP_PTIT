package com.example.oopPTIT.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class FaceEmbeddingCodec {
    public static final String MODEL_NAME = "SFace-2021dec";
    private FaceEmbeddingCodec() {
    }

    /** SFace 128-dimension float32 vector, little-endian; exactly 512 bytes. */
    public static byte[] encode(List<Float> embedding) {
        if (embedding == null || embedding.size() != 128) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned an invalid embedding");
        }
        ByteBuffer bytes = ByteBuffer.allocate(128 * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (Float value : embedding) {
            if (value == null || !Float.isFinite(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned an invalid embedding");
            }
            bytes.putFloat(value);
        }
        return bytes.array();
    }

    public static List<Float> decode(byte[] bytes) {
        if (bytes == null || bytes.length != 128 * Float.BYTES) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored face profile is invalid");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        List<Float> embedding = new ArrayList<>(128);
        while (buffer.hasRemaining()) {
            float value = buffer.getFloat();
            if (!Float.isFinite(value)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored face profile is invalid");
            }
            embedding.add(value);
        }
        return embedding;
    }
}
