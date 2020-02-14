package com.jsong.wiki.shiro.controller;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shiro")
public class ShiroController {


    @RequiresRoles("admin")
    @RequestMapping("/hello")
    public String getHello() {
        return "Hello World";
//        return "../index";
    }
}
