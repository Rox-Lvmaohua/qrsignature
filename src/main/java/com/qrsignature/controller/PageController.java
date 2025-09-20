package com.qrsignature.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    @GetMapping("/signature.html")
    public String signPage() {
        return "forward:/signing.html";
    }

    /**
     * 签名页面 - 支持token参数 (二维码直接跳转)
     * 这个路由会生成一个包含token的完整签名页面URL
     */
    @GetMapping("/sign")
    public String signWithToken(@RequestParam(name = "token", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            // 如果有token参数，重定向到带token的签名页面
            return "redirect:/signature.html?token=" + token;
        } else {
            // 如果没有token参数，重定向到基本签名页面
            return "redirect:/signature.html";
        }
    }

    /**
     * 根路径重定向到主页
     */
    @GetMapping("/home.html")
    public String homePage() {
        return "forward:/index.html";
    }
}