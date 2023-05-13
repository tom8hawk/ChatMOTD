package ru.siaw.chatmotd;

public class ChatMessage {
    private final String senderName;
    private final String message;

    public ChatMessage(String senderName, String message) {
        this.senderName = senderName;
        this.message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }
}
