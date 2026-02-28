package com.msc.service;

import com.msc.model.entity.User;
import com.msc.result.PageResult;

public interface UserService {

    void register(User user);

    String login(String username, String password);

    User findById(Long id);

    User currentUser();

    void updateStatus(Long id, Boolean enabled);

    PageResult<User> page(int page, int size);
}