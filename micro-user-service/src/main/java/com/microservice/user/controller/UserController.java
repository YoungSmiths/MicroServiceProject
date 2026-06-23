package com.microservice.user.controller;

import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.result.Result;
import com.microservice.user.entity.User;
import com.microservice.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("User Service is OK!");
    }

    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.findByUserId(userId);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/username/{username}")
    public Result<User> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        User registered = userService.registerUser(user);
        return Result.success("注册成功", registered);
    }

    @PutMapping
    public Result<Boolean> updateUser(@Valid @RequestBody User user) {
        boolean success = userService.updateUser(user);
        return success ? Result.success("更新成功", true) : Result.error("更新失败");
    }

    @DeleteMapping("/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        boolean success = userService.deleteUser(userId);
        return success ? Result.success("删除成功", true) : Result.error("删除失败");
    }

    @PostMapping("/order-sync")
    public Result<Boolean> syncOrderProfile(@RequestBody UserOrderSyncRequest request) {
        boolean success = userService.syncOrderProfile(request);
        return success ? Result.success("同步用户下单画像成功", true) : Result.error("同步用户下单画像失败");
    }

    @GetMapping("/list")
    public Result<List<User>> listUsers() {
        List<User> users = userService.list();
        return Result.success(users);
    }
}
