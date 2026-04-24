package com.backend.enums.common;

public enum ProcessingResult {
    COB_NOT_FINISHED("View is empty - COB not yet finished", -1),
    ALL_SUCCESS("All SMS processed successfully, zero failures", 0),
    WITH_FAILURES("SMS processed with failures found", 1);

    private final String description;
    private final int code;

    ProcessingResult(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public boolean isCobFinished() {
        return this != COB_NOT_FINISHED;
    }

    public boolean hasFailures() {
        return this == WITH_FAILURES;
    }
}
