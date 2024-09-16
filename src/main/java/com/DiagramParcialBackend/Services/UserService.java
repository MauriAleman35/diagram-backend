package com.DiagramParcialBackend.Services;

import Utils.Role;
import com.DiagramParcialBackend.Dto.UserDto;
import com.DiagramParcialBackend.Entity.Users;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.errors.excepciones.BadRequestException;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    @Autowired
    UserRepository userRepository;

    public Users createUser(UserDto userDto){
        var usersPresent=userRepository.findByEmail(userDto.getEmail());
        System.out.println(usersPresent);
        if (usersPresent.isPresent()){
            throw new BadRequestException(
                    "El usuario con Email"+ usersPresent.get().getEmail()+"se encuentra en el sistema"
            );
        }
        try{
            var passwordEncrypt=new BCryptPasswordEncoder().encode(userDto.getPassword());
            var userNew=new Users(
                    userDto.getEmail(),
                    userDto.getName(),
                    passwordEncrypt,
                    userDto.getRole()
            );
            userRepository.save(userNew);
            return userNew;
        }catch (IllegalArgumentException e){
            throw new NotFoundException("Usuario not found");
        }
    }

    // 4. Obtener un usuario por su ID
    public Users getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id " + id));
    }

    // 5. Actualizar un usuario
    public Users updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id).map(user -> {
            user.setName(userDto.getName());
            user.setEmail(userDto.getEmail());
            user.setRole(userDto.getRole());
            return userRepository.save(user);
        }).orElseThrow(() -> new NotFoundException("Usuario no encontrado con id " + id));
    }

    // 6. Eliminar un usuario
    public Users deleteUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id " + id));
        userRepository.delete(user);

        return user;
    }


}
