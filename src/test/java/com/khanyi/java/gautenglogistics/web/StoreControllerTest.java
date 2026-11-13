package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** Tests only the web layer, with pretend repositories. */
@WebMvcTest(StoreController.class)
class StoreControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private StoreRepository storeRepository;

    @MockitoBean
    private StoreStockRepository stockRepository;

    @Test
    void listsTheStoresByName() {
        Store menlyn = new Store("Menlyn", new Coordinate(-25.7838, 28.2766));
        ReflectionTestUtils.setField(menlyn, "id", 4L);
        given(storeRepository.findAll(Sort.by("name"))).willReturn(List.of(menlyn));

        assertThat(mvc.get().uri("/stores"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id": 4, "name": "Menlyn", "location": {"lat": -25.7838, "lon": 28.2766}}]""");
    }

    @Test
    void showsAStoresStock() {
        given(storeRepository.existsById(4L)).willReturn(true);
        given(stockRepository.findByStoreIdOrderBySku(4L)).willReturn(List.of(
                new StoreStock(4L, "BAG-TOTE", 5), new StoreStock(4L, "HAT-BUCKET", 0)));

        assertThat(mvc.get().uri("/stores/4/stock"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"sku": "BAG-TOTE", "quantity": 5}, {"sku": "HAT-BUCKET", "quantity": 0}]""");
    }

    @Test
    void theStockOfAStoreThatDoesntExistIs404() {
        given(storeRepository.existsById(99L)).willReturn(false);

        assertThat(mvc.get().uri("/stores/99/stock")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
