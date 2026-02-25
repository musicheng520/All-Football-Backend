package com.msc.controller.admin;

import com.msc.model.entity.User;
import com.msc.result.Result;
import com.msc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.findById(id));
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id,
                               @RequestParam Boolean enabled) {
        userService.updateStatus(id, enabled);
        return Result.success();
    }
}