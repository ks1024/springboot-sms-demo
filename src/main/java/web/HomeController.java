package web;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by kyan on 20/06/16.
 */
@Controller
@EnableAutoConfiguration
public class HomeController {

    @RequestMapping("/")
    @ResponseBody
    public String home() {
        return "Hello world!";
    }
}
