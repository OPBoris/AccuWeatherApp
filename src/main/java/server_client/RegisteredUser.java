package server_client;

public class RegisteredUser extends User{

    public RegisteredUser(String username) {
        super(username);
    }

    @Override
    public String getRole() {
        return "USER";
    }
}
