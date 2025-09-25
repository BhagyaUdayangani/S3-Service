package com.example.s3_bucket.tokenmapper.objects;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Role {

    private Long id;
    private String roleName;
    private List<String> permission;

    public Role(Long id) {
        this.id = id;
    }
    public Role( String roleName) {
        this.roleName = roleName;
    }
    public Role(Long id, String roleName, List<String> permission) {
        this.id = id;
        this.permission = permission;
        this.roleName = roleName;
    }
}