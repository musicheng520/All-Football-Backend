package com.msc.service.impl;

import com.msc.context.BaseContext;
import com.msc.mapper.UserMapper;
import com.msc.model.entity.User;
import com.msc.service.UserService;
import com.msc.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(User user) {

        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        // encryption
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userMapper.insert(user);
    }

    @Override
    public String login(String username, String rawPassword) {

        User user = userMapper.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("User disabled");
        }

        // validation
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        // generate JWT
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User currentUser() {
        Long userId = BaseContext.getCurrentId();
        return userMapper.findById(userId);
    }

    @Override
    public void updateStatus(Long id, Boolean enabled) {
        userMapper.updateStatus(id, enabled);
    }
}