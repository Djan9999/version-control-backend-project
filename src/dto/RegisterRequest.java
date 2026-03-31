package dto;

public class RegisterRequest { //данни, които идват от регистрация
    private String username;
    private String password;
    private String role;

    public RegisterRequest() {}

    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public String getRole() {return role;}
}
