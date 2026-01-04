package server_client;

public class RegularUser extends User {

    public RegularUser(String username) {
        super(username);
    }

    @Override
    public String getRole() {
        return "Regular User";
    }
}
