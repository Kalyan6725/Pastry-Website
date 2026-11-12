package org.example.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentSchemaMigration {

    public PaymentSchemaMigration(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_gateway_check");
        jdbcTemplate.execute("ALTER TABLE payments ADD CONSTRAINT payments_gateway_check CHECK (gateway IN ('COD', 'RAZORPAY', 'JUSPAY'))");
        jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check");
        jdbcTemplate.execute("ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check CHECK (payment_method IS NULL OR payment_method IN ('COD', 'UPI', 'CARD', 'NET_BANKING', 'WALLET'))");
    }
}
