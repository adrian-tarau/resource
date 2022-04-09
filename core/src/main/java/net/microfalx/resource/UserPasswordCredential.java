package net.microfalx.resource;

import static net.microfalx.resource.ResourceUtils.*;

public class UserPasswordCredential implements UserAwareCredential {

    private final String userName;
    private final String password;

    public static UserPasswordCredential create(String userName, String password) {
        return new UserPasswordCredential(userName, password);
    }

    public UserPasswordCredential(String userName, String password) {
        this.userName = defaultIfEmpty(userName, "anonymous");
        this.password = defaultIfNull(password, EMPTY_STRING);
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "UserPasswordCredential{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
