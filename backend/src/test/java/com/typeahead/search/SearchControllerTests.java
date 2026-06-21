package com.typeahead.search;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(SearchController.class)
class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    void returns200ForValidRequest() throws Exception {
        when(searchService.search(new SearchRequest("iphone"))).thenReturn(new SearchResponse("Searched"));

        mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"query":"iphone"}
                    """))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"message":"Searched"}
                """));
    }

    @Test
    void returns400ForInvalidRequest() throws Exception {
        when(searchService.search(new SearchRequest("   ")))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be empty"));

        mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"query":"   "}
                    """))
            .andExpect(status().isBadRequest());
    }
}
