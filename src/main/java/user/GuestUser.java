package user;

public class GuestUser extends User {

    public GuestUser() {
        super("Guest_" + System.currentTimeMillis());
    }

    @Override
    public String getRole() {
        return "GUEST";
    }
}
