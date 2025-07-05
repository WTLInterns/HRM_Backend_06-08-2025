package com.jaywant.demo.Entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class MasterAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;
    private String email;
    private String password;
    private String roll = "MASTER_ADMIN";
    private String profileImg;
    private long mobileno;

    private String MasterAdminEmailServerPassword;

    public String getMasterAdminEmailServerPassword() {
        return MasterAdminEmailServerPassword;
    }

    public void setMasterAdminEmailServerPassword(String masterAdminEmailServerPassword) {
        MasterAdminEmailServerPassword = masterAdminEmailServerPassword;
    }

    @OneToMany(mappedBy = "masterAdmin", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Subadmin> subadmin;

    // ✅ Required by JPA
    public MasterAdmin() {
    }

    // ✅ Constructor used in update
    public MasterAdmin(Long id, String name, String email, long mobileno, String roll, String password, String masterAdminEmailServerPassword2) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobileno = mobileno;
        this.roll = roll;
        this.password = password;
    }

    // ✅ Constructor used in registration (without ID)
    public MasterAdmin(String name, String email, long mobileno, String roll, String password) {
        this.name = name;
        this.email = email;
        this.mobileno = mobileno;
        this.roll = roll;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getProfileImg() {
        return profileImg;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public long getMobileno() {
        return mobileno;
    }

    public void setMobileno(long mobileno) {
        this.mobileno = mobileno;
    }

    public List<Subadmin> getSubadmin() {
        return subadmin;
    }

    public void setSubadmin(List<Subadmin> subadmin) {
        this.subadmin = subadmin;
    }

}
