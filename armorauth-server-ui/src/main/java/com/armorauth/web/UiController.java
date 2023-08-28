package com.armorauth.web;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@ArmorAuthController
public class UiController {


    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    @RegisterReflectionForBinding(String.class)
    public String index() {
        return "index";
    }


}