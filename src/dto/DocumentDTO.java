package dto;

public class DocumentDTO {
    public int id;
    public String title;
    public String author;
    public int activeVersionNumber;
    public String status;

    public DocumentDTO(int id, String title, String author, int activeVersionNumber, String status) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.activeVersionNumber = activeVersionNumber;
        this.status = status;
    }
}