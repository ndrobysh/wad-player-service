package com.wad.player.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wad.player.dto.AddMonsterRequest;
import com.wad.player.dto.GainExperienceRequest;
import com.wad.player.model.Player;
import com.wad.player.service.AuthService;
import com.wad.player.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PlayerService playerService;
    @MockBean private AuthService authService;
    @Autowired private ObjectMapper objectMapper;

    private Player sample;
    private final String TOKEN = "Bearer valid-token";
    private final String USER = "player1";

    @BeforeEach
    void setUp() {
        sample = new Player();
        sample.setId("p1");
        sample.setUsername(USER);
        sample.setLevel(1);
        sample.setExperience(0);
        sample.setXpThreshold(50);
        sample.setMaxMonsters(10);
        sample.setMonsters(new ArrayList<>());
    }

    @Test
    void getProfile_returns200() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.getProfile(USER)).thenReturn(sample);
        mockMvc.perform(get("/api/players/profile").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USER))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.maxMonsters").value(10));
    }

    @Test
    void getProfile_returns401() throws Exception {
        when(authService.validateToken("Bearer bad"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        mockMvc.perform(get("/api/players/profile").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPlayerByUsername_returns200() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.getProfile(USER)).thenReturn(sample);
        mockMvc.perform(get("/api/players/player1").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USER))
                .andExpect(jsonPath("$.maxMonsters").value(10));
    }

    @Test
    void getPlayerByUsername_returns403ForOtherUser() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        mockMvc.perform(get("/api/players/otherUser").header("Authorization", TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMonsterIds_returns200() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.getMonsterIds(USER)).thenReturn(List.of("m1", "m2"));
        mockMvc.perform(get("/api/players/monsters").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("m1"))
                .andExpect(jsonPath("$[1]").value("m2"));
    }

    @Test
    void getLevel_returns200() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.getLevel(USER)).thenReturn(5);
        mockMvc.perform(get("/api/players/level").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(5))
                .andExpect(jsonPath("$.username").value(USER));
    }

    @Test
    void gainExperience_returns200() throws Exception {
        sample.setExperience(30);
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.gainExperience(eq(USER), any(GainExperienceRequest.class))).thenReturn(sample);
        mockMvc.perform(post("/api/players/experience").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").value(30));
    }

    @Test
    void addMonster_returns200() throws Exception {
        sample.getMonsters().add("new-m");
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.addMonster(eq(USER), any(AddMonsterRequest.class))).thenReturn(sample);
        mockMvc.perform(post("/api/players/monsters").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monsterId\":\"new-m\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monsters[0]").value("new-m"));
    }

    @Test
    void removeMonster_returns200() throws Exception {
        when(authService.validateToken(TOKEN)).thenReturn(USER);
        when(playerService.removeMonster(USER, "m1")).thenReturn(sample);
        mockMvc.perform(delete("/api/players/monsters/m1").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USER));
    }
}
