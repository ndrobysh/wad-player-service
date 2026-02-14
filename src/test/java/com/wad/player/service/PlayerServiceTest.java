package com.wad.player.service;

import com.wad.player.dto.AddMonsterRequest;
import com.wad.player.dto.GainExperienceRequest;
import com.wad.player.model.Player;
import com.wad.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock private PlayerRepository playerRepository;
    private PlayerService playerService;
    private Player sample;
    private final String USER = "player1";

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, 10, 1, 50, 1.1, 50);
        sample = new Player();
        sample.setId("p1"); sample.setUsername(USER); sample.setLevel(1);
        sample.setExperience(0); sample.setXpThreshold(50);
        sample.setMaxMonsters(10); sample.setMonsters(new ArrayList<>());
    }

    @Test
    void getOrCreatePlayer_returnsExisting() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        Player p = playerService.getOrCreatePlayer(USER);
        assertThat(p.getUsername()).isEqualTo(USER);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void getOrCreatePlayer_createsNewIfMissing() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.empty());
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.getOrCreatePlayer(USER);
        assertThat(p.getLevel()).isEqualTo(1);
        assertThat(p.getXpThreshold()).isEqualTo(50);
        assertThat(p.getMaxMonsters()).isEqualTo(10);
        assertThat(p.getMonsters()).isEmpty();
    }

    @Test
    void getProfile_returnsPlayer() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThat(playerService.getProfile(USER).getUsername()).isEqualTo(USER);
    }

    @Test
    void getMonsterIds_returnsList() {
        sample.getMonsters().add("m1"); sample.getMonsters().add("m2");
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThat(playerService.getMonsterIds(USER)).containsExactly("m1", "m2");
    }

    @Test
    void getLevel_returnsLevel() {
        sample.setLevel(7);
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThat(playerService.getLevel(USER)).isEqualTo(7);
    }

    @Test
    void gainXp_noLevelUp() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.gainExperience(USER, new GainExperienceRequest(30));
        assertThat(p.getExperience()).isEqualTo(30);
        assertThat(p.getLevel()).isEqualTo(1);
    }

    @Test
    void gainXp_levelUp1to2() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.gainExperience(USER, new GainExperienceRequest(50));
        assertThat(p.getLevel()).isEqualTo(2);
        assertThat(p.getExperience()).isEqualTo(0);
        assertThat(p.getXpThreshold()).isEqualTo(55);  // 50*1.1
        assertThat(p.getMaxMonsters()).isEqualTo(11);
    }

    @Test
    void gainXp_levelUp2to3() {
        sample.setLevel(2); sample.setXpThreshold(55); sample.setMaxMonsters(11);
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.gainExperience(USER, new GainExperienceRequest(55));
        assertThat(p.getLevel()).isEqualTo(3);
        assertThat(p.getXpThreshold()).isEqualTo(61); // 55*1.1=60.5 arrondi 61
        assertThat(p.getMaxMonsters()).isEqualTo(12);
    }

    @Test
    void gainXp_multipleLevelUps() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.gainExperience(USER, new GainExperienceRequest(110));
        assertThat(p.getLevel()).isEqualTo(3);
        assertThat(p.getExperience()).isEqualTo(5); // 110-50=60, 60-55=5
        assertThat(p.getMaxMonsters()).isEqualTo(12);
    }

    @Test
    void gainXp_throwsOnNegative() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.gainExperience(USER, new GainExperienceRequest(-1)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("positif");
    }

    @Test
    void gainXp_throwsOnZero() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.gainExperience(USER, new GainExperienceRequest(0)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("positif");
    }

    @Test
    void gainXp_throwsAtMaxLevel() {
        sample.setLevel(50);
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.gainExperience(USER, new GainExperienceRequest(100)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("maximum");
    }

    @Test
    void addMonster_addsToList() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.addMonster(USER, new AddMonsterRequest("m-abc"));
        assertThat(p.getMonsters()).containsExactly("m-abc");
    }

    @Test
    void addMonster_throwsWhenFull() {
        for (int i = 0; i < 10; i++) sample.getMonsters().add("m" + i);
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.addMonster(USER, new AddMonsterRequest("extra")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("maximale");
    }

    @Test
    void addMonster_throwsOnBlankId() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.addMonster(USER, new AddMonsterRequest("")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("requis");
    }

    @Test
    void addMonster_throwsOnNullId() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.addMonster(USER, new AddMonsterRequest(null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("requis");
    }

    @Test
    void removeMonster_removes() {
        sample.getMonsters().add("m1"); sample.getMonsters().add("m2");
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Player p = playerService.removeMonster(USER, "m1");
        assertThat(p.getMonsters()).containsExactly("m2");
    }

    @Test
    void removeMonster_throwsWhenNotFound() {
        when(playerRepository.findByUsername(USER)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> playerService.removeMonster(USER, "nope"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("non trouvé");
    }
}
