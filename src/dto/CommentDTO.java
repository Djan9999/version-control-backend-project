package dto;

public class CommentDTO {
    public int id;
    public String author;
    public String text;

    public CommentDTO(int id, String author, String text) {
        this.id = id;
        this.author = author;
        this.text = text;
    }
}
