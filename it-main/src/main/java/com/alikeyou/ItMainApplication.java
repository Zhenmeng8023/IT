package com.alikeyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.alikeyou.itmoduleuser",
        "com.alikeyou.itmodulecommon",
        "com.alikeyou.itmodulecircle",
        "com.alikeyou.itmoduleinteractive",
        //"com.alikeyou.itmoduleproject",
        "com.alikeyou.itmodulerecommend",
        "com.alikeyou.itmodulesystem",
        "com.alikeyou.itmodulelogin",
        "com.alikeyou.itmoduleblog",

})
public class ItMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItMainApplication.class, args);
    }
}