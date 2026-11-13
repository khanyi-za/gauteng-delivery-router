package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.simulation.OrderGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Tests only the web layer, with a pretend generator. */
@WebMvcTest(GeneratorController.class)
class GeneratorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderGenerator orderGenerator;

    @Test
    void saysWhetherItIsRunning() {
        given(orderGenerator.isRunning()).willReturn(true);

        assertThat(mvc.get().uri("/generator"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"running": true}""");
    }

    @Test
    void startsIt() {
        given(orderGenerator.isRunning()).willReturn(true);

        assertThat(put("""
                {"running": true}"""))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"running": true}""");
        verify(orderGenerator).setRunning(true);
    }

    @Test
    void stopsIt() {
        put("""
                {"running": false}""").exchange();

        verify(orderGenerator).setRunning(false);
    }

    @Test
    void sayingNothingIs400() {
        assertThat(put("{}")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(orderGenerator);
    }

    private MockMvcTester.MockMvcRequestBuilder put(String json) {
        return mvc.put().uri("/generator").contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
