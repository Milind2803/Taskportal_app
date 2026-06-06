package com.milind.taskportal.service;

import com.milind.taskportal.model.User;
import com.milind.taskportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername()))
            throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(user.getEmail()))
            throw new RuntimeException("Email already registered");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (userRepository.count() == 0) user.setRole(User.Role.ADMIN);
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> findAll() { return userRepository.findAll(); }
}
