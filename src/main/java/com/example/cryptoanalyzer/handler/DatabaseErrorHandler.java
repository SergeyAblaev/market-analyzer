package com.example.cryptoanalyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.SpringApplication;
//import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
//import org.h2.jdbc.JdbcException;
//import org.h2.jdbc.DbException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class DatabaseErrorHandler {

//    private final ApplicationContext context;

    @ExceptionHandler(DataAccessException.class)
    public void handleDatabaseError(DataAccessException ex) {
        String message = ex.getMessage();

        // Check for Error H2: Table NOT FOUND
        if (message != null && message.contains("Table \"OHLC_CANDLES\" not found")) {
            log.error("Critical DB ERROR (Table NOT FOUND). Initiating an APPLICATION RESTART!...");

            // Starting shutdown in a new thread for handler stoping correct.
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Giving time for the logs to be written
                } catch (InterruptedException ignored) {}

                // 1. Simple exit (In case of restart policy in Docker/K8s)
                System.exit(1);

                // 2. Or can try to close context.
                // SpringApplication.exit(context, () -> 1);
            }).start();
        }
    }
}