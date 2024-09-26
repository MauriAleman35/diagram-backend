package com.DiagramParcialBackend.Response;

import com.DiagramParcialBackend.Entity.Users;
import lombok.Data;

@Data
public class UserResponse {
    Users users;
    public UserResponse(Users users){
        this.users=users;

    }

}
