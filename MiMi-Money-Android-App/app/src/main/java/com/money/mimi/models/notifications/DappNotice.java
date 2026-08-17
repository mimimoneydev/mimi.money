package com.money.mimi.models.notifications;

public class DappNotice {
    private final String id;
    private final String type;
    private final String title;
    private final String message;
    private final String link;
    private final String image;
    private final long receivedAt;
    private final boolean read;

    public DappNotice(String id, String type, String title, String message, String link, String image, long receivedAt, boolean read) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.image = image;
        this.receivedAt = receivedAt;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public String getImage() {
        return image;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public boolean isRead() {
        return read;
    }
}
