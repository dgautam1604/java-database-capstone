package com.project.back_end.DTO;

public class Login {

    // Fields
    private String identifier;  // Unique identifier (email or username)
    private String password;    // Password for authentication

    // Default constructor (optional but useful for deserialization)
    public Login() {
    }

    // Constructor for easy initialization (optional)
    public Login(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // Getter for 'identifier'
    public String getIdentifier() {
        return identifier;
    }

    // Setter for 'identifier'
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    // Getter for 'password'
    public String getPassword() {
        return password;
    }

    // Setter for 'password'
    public void setPassword(String password) {
        this.password = password;
    }
}
