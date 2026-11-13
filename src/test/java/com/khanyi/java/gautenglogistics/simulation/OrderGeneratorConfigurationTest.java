package com.khanyi.java.gautenglogistics.simulation;

import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Starts a tiny Spring context with just the generator's configuration and pretend beans it needs,
 * to check whether it starts running.
 */
class OrderGeneratorConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Properties.class, OrderGeneratorConfiguration.class)
            .withBean(OrderService.class, () -> mock(OrderService.class))
            .withBean(StoreRepository.class, () -> mock(StoreRepository.class))
            .withBean(ProductRepository.class, () -> mock(ProductRepository.class))
            .withPropertyValues("gautenglogistics.generator.interval=1h"); // so it doesn't run during the test

    @Test
    void isThereButStoppedUnlessSwitchedOn() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(OrderGenerator.class);
            assertThat(context.getBean(OrderGenerator.class).isRunning()).isFalse();
        });
    }

    @Test
    void startsRunningWhenSwitchedOn() {
        runner.withPropertyValues("gautenglogistics.generator.enabled=true")
                .run(context -> assertThat(context.getBean(OrderGenerator.class).isRunning()).isTrue());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OrderGeneratorProperties.class)
    static class Properties {
    }
}
