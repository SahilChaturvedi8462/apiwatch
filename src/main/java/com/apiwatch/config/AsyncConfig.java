package com.apiwatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous method execution (e.g. for non-blocking email alerts).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
