package models;

import java.sql.Timestamp;

public class DocumentVersion {
    private int id;
    private int documentId;
    private int versionNumber;
    private String content;
    private DocumentStatus status;
    private int createdBy; //ID на усера
    private Timestamp createdAt;

    public DocumentVersion(int id,int documentId, int versionNumber, String content, String statusString, int createdBy, Timestamp createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.status = DocumentStatus.valueOf(statusString.toUpperCase());
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getContent() {
        return content;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
