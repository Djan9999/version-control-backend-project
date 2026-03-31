public class User {
    private int id;
    private String username;
    private Role role;

    public User(int id, String username, String roleString)
    {
        this.id = id;
        this.username = username;
        this.role = Role.valueOf(roleString.toUpperCase());
    }

    public int getId()
    {
        return id;
    }

    public  String getUsername()
    {
        return username;
    }

    public Role getRole()
    {
        return role;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setRole(Role role)
    {
        this.role = role;
    }

}
