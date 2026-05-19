package com.admin.adminlfarma_mini.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BIController {

    @GetMapping("/bi")
    public String verTableroBI(Model model) {
        model.addAttribute("activePage", "bi");
        return "bi";
    }
}
