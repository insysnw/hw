package client;

public class User {
    private String userName;
    private boolean nameStatus;

    public User(String userName, boolean nameStatus) {
        this.nameStatus = nameStatus;
        this.userName = userName;
    }


    public boolean getNameStatus() {
        return nameStatus;
    }

    public void setNameStatus(boolean nameStatus) {
        this.nameStatus = nameStatus;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return this.userName.equals(user.userName) && this.nameStatus == user.nameStatus;
    }
}
