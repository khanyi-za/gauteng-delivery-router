package com.khanyi.java.gautenglogistics.simulation;

import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.random.RandomGenerator;

/**
 * Creates the order generator, only when gautenglogistics.generator.enabled=true. It's off by
 * default, so tests and normal runs only get the orders someone places.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty("gautenglogistics.generator.enabled")
class OrderGeneratorConfiguration {

    @Bean
    OrderGenerator orderGenerator(OrderService orderService, StoreRepository storeRepository,
                                  ProductRepository productRepository, OrderGeneratorProperties properties) {
        return new OrderGenerator(orderService, storeRepository, productRepository, properties,
                RandomGenerator.getDefault());
    }
}
