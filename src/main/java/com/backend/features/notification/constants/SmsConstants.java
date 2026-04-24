package com.backend.features.notification.constants;

public final class SmsConstants {

    private SmsConstants() {
    }

    public static final class SmsStatus {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";

        private SmsStatus() {
        }
    }

    public static final class QueueStatus {
        public static final String PENDING = "PENDING";
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";

        private QueueStatus() {
        }
    }
}
