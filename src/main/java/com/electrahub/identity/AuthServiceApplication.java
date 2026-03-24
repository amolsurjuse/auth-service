package com.electrahub.identity;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceApplication.class);


	/**
	 * Executes main for `AuthServiceApplication`.
	 *
	 * <p>Detailed behavior: follows the current implementation path and
	 * enforces component-specific rules in `com.electrahub.identity`.
	 * @param args input consumed by main.
	 */
	public static void main(String[] args) {
	    LOGGER.info("CODEx_ENTRY_LOG: Entering AuthServiceApplication#main");
	    LOGGER.debug("CODEx_ENTRY_LOG: Entering AuthServiceApplication#main with debug context");
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
