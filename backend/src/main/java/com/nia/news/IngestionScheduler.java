package com.nia.news;

import com.nia.config.NiaProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Runs the ingestion pass on a cron schedule, set by {@code NIA_INGEST_CRON}
 * (default every 2 hours). No message broker or worker pool — a single
 * scheduled job is sufficient.
 */
@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final IngestionService ingestionService;
    private final NiaProperties props;

    public IngestionScheduler(IngestionService ingestionService, NiaProperties props) {
        this.ingestionService = ingestionService;
        this.props = props;
    }

    /**
     * Logs the parsed schedule and the next few fire times at startup, so the
     * cadence can be confirmed from the logs instead of assumed from config.
     */
    @PostConstruct
    void logSchedule() {
        String cron = props.getIngest().getCron();
        if (!props.getIngest().isEnabled()) {
            log.warn("NIA ingestion is DISABLED (NIA_INGEST_ENABLED=false); no news will be fetched.");
            return;
        }
        try {
            CronExpression expression = CronExpression.parse(cron);
            LocalDateTime next = LocalDateTime.now();
            StringBuilder upcoming = new StringBuilder();
            for (int i = 0; i < 3 && next != null; i++) {
                next = expression.next(next);
                upcoming.append(i == 0 ? "" : ", ").append(next);
            }
            log.info("NIA ingestion scheduled | cron='{}' | next runs: {}", cron, upcoming);
        } catch (IllegalArgumentException ex) {
            log.error("NIA_INGEST_CRON '{}' is not a valid cron expression - ingestion will NOT run.", cron);
        }
    }

    @Scheduled(cron = "${nia.ingest.cron}")
    public void scheduledIngest() {
        if (!props.getIngest().isEnabled()) {
            return;
        }
        try {
            ingestionService.ingestAll();
        } catch (Exception ex) {
            log.error("Scheduled ingestion failed: {}", ex.getMessage());
        }
    }
}
