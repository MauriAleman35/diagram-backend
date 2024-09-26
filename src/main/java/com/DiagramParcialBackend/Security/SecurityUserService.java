package com.DiagramParcialBackend.Security;

import com.DiagramParcialBackend.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserService implements UserDetailsService {
    private final UserRepository userRepository;

    public SecurityUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }



    @Override
    public UserDetails loadUserByUsername(String username)   {
        var optUser = this.userRepository.findByEmail(username);
        if(optUser.isPresent()){
            return new SecurityUser(optUser.get());
        }
        throw new UsernameNotFoundException("User not found"+ username);
    }






}