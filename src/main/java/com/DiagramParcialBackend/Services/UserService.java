package com.DiagramParcialBackend.Services;

import com.DiagramParcialBackend.Dto.UserDto;
import com.DiagramParcialBackend.Entity.Users;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.errors.excepciones.BadRequestException;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Transactional
    public Users createUser(UserDto userDto) {
        // Verificar si el usuario ya existe
        var usersPresent = userRepository.findByEmail(userDto.getEmail());

        if (usersPresent.isPresent()) {
            throw new BadRequestException(
                    "El usuario con el Email " + usersPresent.get().getEmail() + " ya se encuentra en el sistema"
            );
        }

        try {
            // Cifrar la contraseña
            var passwordEncrypt = new BCryptPasswordEncoder().encode(userDto.getPassword());

            // Crear el nuevo usuario
            var userNew = new Users(
                    userDto.getEmail(),
                    userDto.getName(),
                    passwordEncrypt
            );

            // Guardar el usuario en la base de datos
            return userRepository.save(userNew);

        } catch (Exception e) {
            System.err.println("Error al crear el usuario: " + e.getMessage());
            throw new RuntimeException("Ocurrió un error al crear el usuario: " + e.getMessage());
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
