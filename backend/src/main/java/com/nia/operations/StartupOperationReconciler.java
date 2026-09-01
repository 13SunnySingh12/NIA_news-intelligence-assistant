package com.nia.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, marks any operations left in PENDING/RUNNING as FAILED. After a
 * restart no in-memory task is actually running, so those rows are stale — this
 * prevents the frontend from showing a "stuck" operation that will never finish.
 */
@Component
public class StartupOperationReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupOperationReconciler.class);

    private final OperationService operationService;

    public StartupOperationReconciler(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            operationService.failOpenOperationsOnStartup();
        } catch (Exception ex) {
            // Don't block startup on a transient DB hiccup; log and continue.
            log.warn("Could not reconcile interrupted operations on startup: {}", ex.getMessage());
        }
    }
}
