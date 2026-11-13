package com.khanyi.java.gautenglogistics.web;

/**
 * Whether random demo orders are being placed, as JSON. GET /generator sends it, and PUT
 * /generator takes it to start or stop them.
 */
public record GeneratorState(boolean running) {
}
