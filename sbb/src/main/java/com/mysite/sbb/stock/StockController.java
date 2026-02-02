package com.mysite.sbb.stock;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StockController {

    @GetMapping("/stock")
    public String stockPage() {
        return "stock";
    }
}
