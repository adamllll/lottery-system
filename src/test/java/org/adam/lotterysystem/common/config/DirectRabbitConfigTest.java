package org.adam.lotterysystem.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectRabbitConfigTest {

    private final DirectRabbitConfig config = new DirectRabbitConfig();

    @Test
    void directExchangeShouldUseBusinessExchangeName() {
        DirectExchange exchange = config.directExchange();

        assertEquals(DirectRabbitConfig.EXCHANGE_NAME, exchange.getName());
    }

    @Test
    void dlxExchangeShouldUseDeadLetterExchangeName() {
        DirectExchange exchange = config.dlxExchange();

        assertEquals(DirectRabbitConfig.DLX_EXCHANGE_NAME, exchange.getName());
    }
}
