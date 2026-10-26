package com.example.oopPTIT.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class FaceEmbeddingUtil {

    private FaceEmbeddingUtil() {}

    public static byte[] toBytes(
            List<Double> embedding
    ) {

        if (embedding == null
                || embedding.isEmpty()
                || embedding.size() > 2048) {
            throw new IllegalArgumentException(
                    "Invalid embedding size"
            );
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(embedding.size() * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (Double value : embedding) {

            if (value == null
                    || !Double.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Invalid embedding value"
                );
            }

            buffer.putFloat(value.floatValue());
        }

        return buffer.array();
    }

    public static float[] fromBytes(
            byte[] bytes
    ) {

        if (bytes == null
                || bytes.length == 0
                || bytes.length % 4 != 0) {
            throw new IllegalArgumentException(
                    "Invalid stored embedding"
            );
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        float[] result =
                new float[bytes.length / 4];

        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.getFloat();
        }

        return result;
    }

    public static double cosineSimilarity(
            float[] a,
            float[] b
    ) {

        if (a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException(
                    "Embedding dimensions differ"
            );
        }

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {

            dot += a[i] * b[i];

            normA += a[i] * a[i];

            normB += b[i] * b[i];
        }

        if (normA <= 0 || normB <= 0) {
            throw new IllegalArgumentException(
                    "Invalid zero vector"
            );
        }

        return dot / (
                Math.sqrt(normA)
                        * Math.sqrt(normB)
        );
    }
}