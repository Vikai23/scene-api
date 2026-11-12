package com.vinicius.scene_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SceneApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SceneApiApplication.class, args);
	}

}
