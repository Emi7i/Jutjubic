package isa.jutjub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Home", description = "Home and general information endpoints")
public class HomeController {

    @Operation(summary = "Get home page information", description = "Returns available endpoints and application information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved home information")
    })
    @GetMapping("/")
    public String home() {
        return "Jutjubic Spring Boot Application is running! 🚀\n\nAvailable endpoints:\n" +
               "GET /api/ - Home (this endpoint)\n" +
               "GET /api/assets - Get all assets\n" +
               "POST /api/person - Create person (with validation)\n" +
               "GET /api/h2-console - H2 Database Console\n" +
               "GET /api/swagger-ui.html - Swagger Documentation\n" +
               "GET /api/api-docs - OpenAPI JSON specification\n\n" +
               "Try: http://localhost:8080/api/assets";
    }
}
