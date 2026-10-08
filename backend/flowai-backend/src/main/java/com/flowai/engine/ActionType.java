package com.flowai.engine;

public enum ActionType {
    SEND_EMAIL,
    SEND_SLACK,
    HTTP_REQUEST,
    AI_PROMPT
    ;

    public static ActionType from(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ação desconhecida: " + s);
        }
    }
}