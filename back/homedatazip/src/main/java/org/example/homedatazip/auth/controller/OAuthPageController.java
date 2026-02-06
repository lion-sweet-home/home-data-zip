package org.example.homedatazip.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthPageController {

    @GetMapping("/oauth/success")
    public String oauthSuccess() {
        return "forward:/oauth/success.html";
    }

    @GetMapping("/oauth/failure")
    public String oauthFailure() {
        return "forward:/oauth/failure.html";
    }
}
