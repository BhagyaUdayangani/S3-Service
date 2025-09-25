package com.example.s3_bucket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class S3BucketApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3BucketApplication.class, args);
	}

}
