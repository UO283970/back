package tfg.books.back.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class postController {
    
    @GetMapping("/greet")
    public String getMethodName() {
        return "Hellow world!!";
    }
    
}
