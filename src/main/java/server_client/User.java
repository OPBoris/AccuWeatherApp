package server_client;

public abstract class User {
    protected String username;

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public abstract String getRole();

    @Override
    public String toString() {
        return "User: " + username + " (" + getRole() + ")";
    }
}
