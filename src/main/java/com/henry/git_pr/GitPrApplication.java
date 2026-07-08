package com.henry.git_pr;


import com.embabel.agent.config.annotation.EnableAgents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;



@SpringBootApplication

public class GitPrApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitPrApplication.class, args);
	}


//	@Bean
//	public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
//		return new Jackson2ObjectMapperBuilder();
//	}

}
