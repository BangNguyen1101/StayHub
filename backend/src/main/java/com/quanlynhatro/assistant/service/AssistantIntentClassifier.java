package com.quanlynhatro.assistant.service;

public interface AssistantIntentClassifier {
    AssistantIntent classify(String message, String role);
}
