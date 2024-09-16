package com.DiagramParcialBackend.Services.Security;

import Security.SecurityUser;
import com.DiagramParcialBackend.Dto.LoginDto;
import com.DiagramParcialBackend.Repository.UserRepository;
import com.DiagramParcialBackend.Response.AuthResponse;
import com.DiagramParcialBackend.errors.excepciones.NotFoundException;
import com.DiagramParcialBackend.errors.excepciones.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final JwtService jwtService;
    @Autowired
    private  final AuthenticationManager authenticationManager;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginDto loginDto){
        var user=this.userRepository.findByEmail(loginDto.getEmail());

        if (!user.isPresent()){
            throw new NotFoundException("No existe el Usuario para logear");
        }

        if(!passwordEncoder.matches(loginDto.getPassword(),user.get().getPassword())){
            throw new UnauthorizedException("Contraseña Incorrecta");
        }

        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(),loginDto.getPassword()));

        }catch (Exception e){
            throw new UnauthorizedException(e.getLocalizedMessage());
        }

        UserDetails userDetails = user.map(SecurityUser::new).orElseThrow();
        String token = jwtService.getToken(userDetails);


        return new AuthResponse(
                token,
                userDetails

        );
    }
}
