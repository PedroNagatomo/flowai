package com.flowai.engine;

public record ActionResult(
        boolean success,
        String message
) {
    public static ActionResult success(String msg) {
        return new ActionResult(true, msg);
    }
    public static ActionResult failure(String msg) {
        return new ActionResult(false, msg);
    }
}