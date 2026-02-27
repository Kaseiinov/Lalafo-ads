package com.example.demo.controller;

import com.example.demo.service.LalafoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class LalafoController {

    private final LalafoService lalafoService;

    @GetMapping("/test-result")
    public String testResult(Model model) {

        return "ads";
    }

}
