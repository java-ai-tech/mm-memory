package com.glmapper.memory;

import com.glmapper.memory.config.ArtisanMemoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Classname Application
 * @Description Application
 * @Date 1/22/26 9:52 PM
 * @Created by glmapper
 */
@SpringBootApplication
@EnableConfigurationProperties(ArtisanMemoryProperties.class)
public class Application {


    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Application.class, args);
        String property = run.getEnvironment().getProperty("artisan.memory.compression.auto-compression");
        System.out.printf(property);
    }
}
