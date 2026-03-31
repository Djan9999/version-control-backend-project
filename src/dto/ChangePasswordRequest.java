package dto;

public class ChangePasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;

    public ChangePasswordRequest() {}

    public String getUsername() { return username; }
    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
}
