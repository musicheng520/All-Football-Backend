package com.msc.controller.user;

import com.msc.model.dto.LoginRequestDTO;
import com.msc.model.entity.User;
import com.msc.result.Result;
import com.msc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        userService.register(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequestDTO dto) {

        String token = userService.login(
                dto.getUsername(),
                dto.getPassword()
        );

        return Result.success(token);
    }

    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(userService.currentUser());
    }
}