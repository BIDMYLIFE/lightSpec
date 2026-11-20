package com.lightspec.auth.web.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SpaControllerTests {

    private final MockMvc mockMvc = standaloneSetup(new SpaController()).build();

    @Test
    void deepLinkForwardsToReactIndex() throws Exception {
        mockMvc.perform(get("/setup/admin"))
                .andExpect(forwardedUrl("/index.html"));
    }
}
