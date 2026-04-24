package com.backend.features.notification.constants;

public final class NotificationConstants {

    private NotificationConstants() {
    }

    public static final class NotificationStatus {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";

        private NotificationStatus() {
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
