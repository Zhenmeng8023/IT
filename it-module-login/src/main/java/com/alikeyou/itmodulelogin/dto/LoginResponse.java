package com.alikeyou.itmodulelogin.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private Other other;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String message, String token, Integer roleId) {
        this.success = success;
        this.message = message;
        this.other = new Other(token, roleId);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Other getOther() {
        return other;
    }

    public void setOther(Other other) {
        this.other = other;
    }

    public static class Other {
        private String token;
        private Integer roleId;

        public Other(String token) {
            this.token = token;
        }

        public Other(String token, Integer roleId) {
            this.token = token;
            this.roleId = roleId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Integer getRoleId() {
            return roleId;
        }

        public void setRoleId(Integer roleId) {
            this.roleId = roleId;
        }
    }
}