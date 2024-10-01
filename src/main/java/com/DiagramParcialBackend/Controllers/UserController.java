package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.UserDto;
import com.DiagramParcialBackend.Entity.Users;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.Repository.UserSessionRepository;
import com.DiagramParcialBackend.Response.ApiResponse;
import com.DiagramParcialBackend.Response.UserResponse;
import com.DiagramParcialBackend.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    UserSessionRepository userSessionRepository;

    @Autowired
    UserService userService;

    @PostMapping("/addUser")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> postUser(@Valid @RequestBody UserDto userDto){
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Usuario Creado",
                new UserResponse(this.userService.createUser(userDto))
        );
    }

    @GetMapping("/{id}")
    public Users getByIdUser(@PathVariable Long id){
        Users users=this.userService.getUserById(id);
        return users;
    }

    @GetMapping("/byEmail")
    public ApiResponse<UserResponse> getByEmail(@RequestParam String email){
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "Usuario Encontrado",
                new UserResponse(this.userService.UserByEmail(email))
        );
    }
}
