package com.example.pdac_assignment;

public class ExecutionContent{
    final int previewFormat;
    final int width;
    final int height;
    final byte[] bytes;

    public ExecutionContent(int previewFormat, int width, int height, byte[] bytes) {
        this.previewFormat = previewFormat;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
    }
}
