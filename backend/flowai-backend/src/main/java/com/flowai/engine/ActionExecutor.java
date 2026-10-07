package com.flowai.engine;

public interface ActionExecutor {
    ActionType supportedType();
    ActionResult execute(ActionConfig config, ExecutionContext ctx);
}