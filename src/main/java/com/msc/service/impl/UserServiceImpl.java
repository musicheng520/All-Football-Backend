package com.msc.service.impl;

import com.msc.mapper.UserMapper;
import com.msc.model.entity.User;
import com.msc.result.PageResult;
import com.msc.service.UserService;
import com.msc.utils.JwtUtil;
import com.msc.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(User user) {

        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setEnabled(true);
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

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        // generate JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // store token in Redis (1 hour)
        redisTemplate.opsForValue()
                .set("login:" + user.getId(), token, 1, TimeUnit.HOURS);

        return token;
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User currentUser() {

        Long userId = ThreadLocalUtil.get();

        if (userId == null) {
            throw new RuntimeException("Not logged in");
        }

        return userMapper.findById(userId);
    }

    @Override
    public void updateStatus(Long id, Boolean enabled) {
        userMapper.updateStatus(id, enabled);
    }

    @Override
    public PageResult<User> page(int page, int size) {

        int offset = (page - 1) * size;

        long total = userMapper.count();

        List<User> records = userMapper.page(offset, size);

        return new PageResult<>(
                total,
                page,
                size,
                records
        );
    }

    @Override
    public void updateAvatar(String avatarUrl) {

        Long userId = ThreadLocalUtil.get();

        if (userId == null) {
            throw new RuntimeException("Not logged in");
        }

        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new RuntimeException("Avatar url is empty");
        }

        userMapper.updateAvatar(userId, avatarUrl);
    }
}