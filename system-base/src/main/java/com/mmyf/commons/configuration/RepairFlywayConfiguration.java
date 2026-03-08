package com.mmyf.commons.configuration;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
//@AutoConfiguration(before = FlywayAutoConfiguration.FlywayConfiguration.class)
public class RepairFlywayConfiguration {


    @Bean
    public FlywayMigrationStrategy migrationStrategy() {
        return flyway -> {
            org.flywaydb.core.api.configuration.Configuration configuration = flyway.getConfiguration();
            flyway.repair();
            flyway.migrate();
        };
    }

    @Component
    public static class RepairBeforeCallback extends BaseCallback {

        @Override
        public boolean supports(Event event, Context context) {
            return event == Event.BEFORE_REPAIR;
        }

        /**
         * Handles this Flyway lifecycle event.
         *
         * @param event   The event to handle.
         * @param context The context for this event.
         */
        @Override
        public void handle(Event event, Context context) {
            MigrationInfo migrationInfo = context.getMigrationInfo();
        }
    }
}