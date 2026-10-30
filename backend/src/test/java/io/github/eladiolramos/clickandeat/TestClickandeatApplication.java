package io.github.eladiolramos.clickandeat;

import org.springframework.boot.SpringApplication;

public class TestClickandeatApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClickandeatApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
