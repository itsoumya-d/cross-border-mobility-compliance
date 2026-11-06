package com.soumyadebnath.mobility.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MobilityControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void statusIsOk() throws Exception {
        mvc.perform(get("/api/v1/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void nextActionsReturnsA1ForCrossBorderTrip() throws Exception {
        mvc.perform(get("/api/v1/trips/t1/next-actions")
                .param("origin", "DE")
                .param("destination", "FR")
                .param("durationDays", "14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].documentType").exists());
    }
}
