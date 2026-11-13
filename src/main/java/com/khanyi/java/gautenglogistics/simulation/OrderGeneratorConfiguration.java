package com.khanyi.java.gautenglogistics.simulation;

import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.random.RandomGenerator;

/**
 * Creates the order generator. It's always there, so the map page can start it, but it only places
 * orders while it's running, and it starts stopped unless gautenglogistics.generator.enabled=true.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class OrderGeneratorConfiguration {

    @Bean
    OrderGenerator orderGenerator(OrderService orderService, StoreRepository storeRepository,
                                  ProductRepository productRepository, OrderGeneratorProperties properties) {
        return new OrderGenerator(orderService, storeRepository, productRepository, properties,
                RandomGenerator.getDefault());
    }
}
