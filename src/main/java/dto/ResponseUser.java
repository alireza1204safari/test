package dto;

import entity.Role;

public class ResponseUser {
    public String email;
    public String username;
    public String role;
    public String full_name;
    public String phone;
    public String address;
    private String profileImageBase64;
    private ResponseBankInfo bankInfo;

    public ResponseUser(String email,String username, String role, String full_name, String phone, String address, String profileImageBase64, ResponseBankInfo bankInfo) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.full_name = full_name;
        this.phone = phone;
        this.address = address;
        this.profileImageBase64 = profileImageBase64;
        this.bankInfo = bankInfo;
    }

    public ResponseBankInfo getBankInfo() {
        return bankInfo;
    }

    public void setBankInfo(ResponseBankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

}
