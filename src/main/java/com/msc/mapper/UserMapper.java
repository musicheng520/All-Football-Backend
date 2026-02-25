package com.msc.mapper;

import com.msc.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    User findById(Long id);

    void insert(User user);

    void updateStatus(@Param("id") Long id,
                      @Param("enabled") Boolean enabled);
}