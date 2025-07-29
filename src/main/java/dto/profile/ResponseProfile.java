package dto.profile;

import dto.ResponseUser;

public class ResponseProfile {
    private String message;
    private ResponseUser user;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResponseUser getUser() {
        return user;
    }

    public void setUser(ResponseUser user) {
        this.user = user;
    }

    public ResponseProfile(String message, ResponseUser user) {
        this.message = message;
        this.user = user;
    }
}