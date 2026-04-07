package domain;

public class User {

    private final int userId;
    private final String username;
    private final String fullName;
    private final String user_role;

    public User(int userId, String username, String fullName, String user_role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.user_role = user_role;
    }

    public int getUserId()     { return userId; }
    public String getUsername()  { return username; }
    public String getFullName()  { return fullName; }
    public String getRole()      { return user_role; }

    public boolean isAdmin()       { return user_role.equals("ADMIN"); }
    public boolean isManager()     { return user_role.equals("MANAGER"); }
    public boolean isPharmacist()  { return user_role.equals("PHARMACIST"); }
}