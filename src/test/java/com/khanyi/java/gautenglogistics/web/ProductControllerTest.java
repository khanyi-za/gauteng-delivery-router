package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Product;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** Tests only the web layer, with a pretend ProductRepository. */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void listsTheProductsByName() {
        given(productRepository.findAll(Sort.by("name"))).willReturn(List.of(new Product("HAT-BUCKET", "Bucket hat")));

        assertThat(mvc.get().uri("/products"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"sku": "HAT-BUCKET", "name": "Bucket hat"}]""");
    }
}
