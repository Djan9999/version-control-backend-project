package dto;

import java.util.List;

public class DocumentDetailDTO {
    public int id;
    public String title;
    public String author;
    public int activeVersionNumber;
    public VersionDTO activeVersion;
    public List<VersionDTO> versions;


    public static class VersionDTO {
        public int versionNumber;
        public String status;
        public String content;

        public VersionDTO(int vNum, String status, String content) {
            this.versionNumber = vNum;
            this.status = status;
            this.content = content;
        }
    }
}
