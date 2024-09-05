package mentordualselectionsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

@RestController
public class HelloWorldController {

    @Operation(summary = "Hello World 接口", description = "这是一个测试的 Hello World API，不需要登录即可访问")
    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello, World!";
    }
}
