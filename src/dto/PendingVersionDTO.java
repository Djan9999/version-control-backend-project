package dto;

public class PendingVersionDTO {
    public int documentId;
    public String documentTitle;
    public int versionNumber;
    public String author;
    public String content;
    public String createdAt;

    public PendingVersionDTO(int documentId, String documentTitle, int versionNumber, String author, String content, String createdAt) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.versionNumber = versionNumber;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }
}
