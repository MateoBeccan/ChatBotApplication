package com.ajsd.chatbot.model;

import java.util.ArrayList;
import java.util.List;

public class ConversationContext {
    private String currentStep; // e.g., "ASK_INTENT", "SELECT_TOPIC", "SELECT_COUNTRY", "SELECT_CONDITION", "CHOOSE_OPTION"
    private String selectedCountry; // Country currently being discussed
    private String selectedCondition; // Medical condition currently being discussed
    private String currentTopic; // "COUNTRIES" or "MEDICINE"
    private List<String> availableOptions; // Options available for the current step
    private List<Message> messages; // Conversation history

    public static class Message {
        private String sender; // "USER" or "BOT"
        private String content;

        public Message(String sender, String content) {
            this.sender = sender;
            this.content = content;
        }

        public String getSender() { return sender; }
        public String getContent() { return content; }
    }

    public ConversationContext() {
        this.currentStep = "ASK_INTENT";
        this.availableOptions = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    // Getters and Setters
    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getSelectedCountry() {
        return selectedCountry;
    }

    public void setSelectedCountry(String selectedCountry) {
        this.selectedCountry = selectedCountry;
    }

    public List<String> getAvailableOptions() {
        return availableOptions;
    }

    public void setAvailableOptions(List<String> availableOptions) {
        this.availableOptions = availableOptions;
    }

    public void addOption(String option) {
        this.availableOptions.add(option);
    }

    public void clearOptions() {
        this.availableOptions.clear();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void addMessage(String sender, String content) {
        this.messages.add(new Message(sender, content));
    }

    public String getSelectedCondition() {
        return selectedCondition;
    }

    public void setSelectedCondition(String selectedCondition) {
        this.selectedCondition = selectedCondition;
    }

    public String getCurrentTopic() {
        return currentTopic;
    }

    public void setCurrentTopic(String currentTopic) {
        this.currentTopic = currentTopic;
    }

    public void clear() {
        this.currentStep = "ASK_INTENT";
        this.selectedCountry = null;
        this.selectedCondition = null;
        this.currentTopic = null;
        this.availableOptions.clear();
        this.messages.clear();
    }
}