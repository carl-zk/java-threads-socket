package org.example.chapter3;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author carl
 */
@RestController
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello world ffs";
    }

    @GetMapping("/abc")
    public String abc() {
        return "abc";
    }
}
