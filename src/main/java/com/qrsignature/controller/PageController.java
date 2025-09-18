package com.qrsignature.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Administrator
 * 静态页面控制器
 */
@Controller
public class PageController {

    /**
     * 主页
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * 签名页面 - 支持token参数
     */
    @GetMapping("/sign.html")
    public String signPage() {
        return "forward:/signing.html";
    }

    /**
     * 根路径重定向到主页
     */
    @GetMapping("/home.html")
    public String homePage() {
        return "forward:/index.html";
    }
}