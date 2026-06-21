package com.typeahead.suggest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SuggestionController.class)
class SuggestionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuggestionService suggestionService;

    @Test
    void returnsHttp200ForMissingQueryParameter() throws Exception {
        when(suggestionService.suggest(null))
            .thenReturn(new SuggestionResponse("", 0, List.of(), "postgres"));

        mockMvc.perform(get("/suggest"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"prefix":"","count":0,"suggestions":[],"source":"postgres"}
                """));
    }

    @Test
    void returnsExpectedJsonShape() throws Exception {
        when(suggestionService.suggest("iph"))
            .thenReturn(new SuggestionResponse(
                "iph",
                1,
                List.of(new SuggestionItem("iphone", 249943L)),
                "postgres"
            ));

        mockMvc.perform(get("/suggest").param("q", "iph"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "prefix":"iph",
                  "count":1,
                  "suggestions":[
                    {
                      "query":"iphone",
                      "count":249943
                    }
                  ],
                  "source":"postgres"
                }
                """));
    }
}
