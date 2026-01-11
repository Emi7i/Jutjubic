package isa.jutjub.controller.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Tag(name = "Registration", description = "Preliminary registration page")
public class RegistrationController {

    @GetMapping("/register")
    public String registrationPage() {
        return "userAuthentication/registration";
    }
}
