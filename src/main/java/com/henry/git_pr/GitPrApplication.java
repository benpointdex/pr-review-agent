package com.henry.git_pr;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync

public class GitPrApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitPrApplication.class, args);
	}


//	@Bean
//	public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
//		return new Jackson2ObjectMapperBuilder();
//	}

}
