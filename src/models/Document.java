package models;

import java.sql.Timestamp;
import java.util.List;

public class Document {
    private int id;
    private String title;
    private int createdBy; //ID на създателя
    private Timestamp createdAt;

    private List<DocumentVersion> history; //Държи всички версии


    public Document(int id, String title, int createdBy, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setHistory(List<DocumentVersion> history) {
        this.history = history;
    }

    public List<DocumentVersion> getHistory() {
        return history;
    }
}
