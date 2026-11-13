package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.simulation.OrderGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /generator says whether random demo orders are being placed; PUT /generator starts or stops them. */
@RestController
@RequestMapping("/generator")
class GeneratorController {

    private final OrderGenerator orderGenerator;

    GeneratorController(OrderGenerator orderGenerator) {
        this.orderGenerator = orderGenerator;
    }

    @GetMapping
    GeneratorState state() {
        return new GeneratorState(orderGenerator.isRunning());
    }

    @PutMapping
    GeneratorState change(@RequestBody GeneratorState state) {
        orderGenerator.setRunning(state.running());
        return state();
    }
}
