package com.hms.model;

import java.io.Serializable;

/** Logged-in user stored in the HTTP session under the name "user". */
public class AppUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private final String fullName;
    private final String role;
    private final String doctorId;

    public AppUser(String id, String username, String fullName, String role, String doctorId) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.doctorId = doctorId;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getDoctorId() { return doctorId; }
}
