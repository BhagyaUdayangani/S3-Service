package com.example.s3_bucket.entity;

import com.example.s3_bucket.enums.AuthProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
@Setter
public class SpordUser extends User {

    private String deviceId;
    private String device;
    private AuthProvider authProvider;
    private String phoneNumber;
    private String email;

    public SpordUser(String username, String password,
                     Collection<? extends GrantedAuthority> authorities,
                     String deviceId, String device, AuthProvider authProvider,
                     String phoneNumber, String email) {
        super(username, password, authorities);
        this.deviceId = deviceId;
        this.device = device;
        this.authProvider = authProvider;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    @Override
    public String toString() {
        return "SpordUser{" +
                "deviceId='" + deviceId + '\'' +
                ", device='" + device + '\'' +
                '}';
    }
}
