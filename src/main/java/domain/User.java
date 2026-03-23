package domain;

public class User {

    private int userId;
    private String username;
    private String fullName;
    private String role;

    public User(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId()     { return userId; }
    public String getUsername()  { return username; }
    public String getFullName()  { return fullName; }
    public String getRole()      { return role; }

    public boolean isAdmin()       { return role.equals("ADMIN"); }
    public boolean isManager()     { return role.equals("MANAGER"); }
    public boolean isPharmacist()  { return role.equals("PHARMACIST"); }
}