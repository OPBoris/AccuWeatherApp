package user;

public class RegularUser extends User {

    public RegularUser(String username) {
        super(username);
    }

    @Override
    public String getRole() {
        return "Regular User";
    }
}
