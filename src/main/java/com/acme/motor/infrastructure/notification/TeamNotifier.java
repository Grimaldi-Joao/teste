package com.acme.motor.infrastructure.notification;

public interface TeamNotifier {
    void notify(String destination, String summary);
}
