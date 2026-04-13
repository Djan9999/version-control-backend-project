package models;

public class RoleRequest {
    public int id;
    public int userId;
    public String username;
    public String currentRole;
    public String requestedRole;
    public String status;
    public String createdAt;

    public RoleRequest(int id, String username, String currentRole, String requestedRole, String status, String createdAt) {
        this.id = id;
        this.username = username;
        this.currentRole = currentRole;
        this.requestedRole = requestedRole;
        this.status = status;
        this.createdAt = createdAt;
    }
}
