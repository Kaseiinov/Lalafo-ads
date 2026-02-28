package com.example.demo.controller;

import com.example.demo.model.Ad;
import com.example.demo.service.LalafoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@RequestMapping
@RequiredArgsConstructor
public class LalafoController {

    private final LalafoService lalafoService;

    @GetMapping("/test-result")
    public String testResult(Model model) {

        List<Ad> ads =  lalafoService.fetchAds();
        model.addAttribute("ads", ads);
        model.addAttribute("total", ads.size());

        return "ads";
    }

}
