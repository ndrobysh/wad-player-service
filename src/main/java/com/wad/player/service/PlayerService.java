package com.wad.player.service;

import com.wad.player.dto.AddMonsterRequest;
import com.wad.player.dto.GainExperienceRequest;
import com.wad.player.model.Player;
import com.wad.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final int initialMonsterSlots;
    private final int slotsPerLevel;
    private final int initialXpThreshold;
    private final double xpMultiplier;
    private final int maxLevel;

    public PlayerService(PlayerRepository playerRepository,
                         @Value("${player.initial-monster-slots:10}") int initialMonsterSlots,
                         @Value("${player.slots-per-level:1}") int slotsPerLevel,
                         @Value("${player.initial-xp-threshold:50}") int initialXpThreshold,
                         @Value("${player.xp-multiplier:1.1}") double xpMultiplier,
                         @Value("${player.max-level:50}") int maxLevel) {
        this.playerRepository = playerRepository;
        this.initialMonsterSlots = initialMonsterSlots;
        this.slotsPerLevel = slotsPerLevel;
        this.initialXpThreshold = initialXpThreshold;
        this.xpMultiplier = xpMultiplier;
        this.maxLevel = maxLevel;
    }

    public Player getOrCreatePlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseGet(() -> createNewPlayer(username));
    }

    private Player createNewPlayer(String username) {
        Player p = new Player();
        p.setUsername(username);
        p.setLevel(1);
        p.setExperience(0);
        p.setXpThreshold(initialXpThreshold);
        p.setMaxMonsters(initialMonsterSlots);
        p.setMonsters(new ArrayList<>());
        return playerRepository.save(p);
    }

    public Player getProfile(String username) {
        return getOrCreatePlayer(username);
    }

    public List<String> getMonsterIds(String username) {
        return getOrCreatePlayer(username).getMonsters();
    }

    public int getLevel(String username) {
        return getOrCreatePlayer(username).getLevel();
    }

    /**
     * Sujet : 50 XP level 1->2, puis seuil * 1.1 a chaque level.
     * Slots monstres : commence a 10, +1 par level.
     */
    public Player gainExperience(String username, GainExperienceRequest request) {
        Player p = getOrCreatePlayer(username);

        if (request.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant d'XP doit être positif");
        }
        if (p.getLevel() >= maxLevel) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau maximum atteint");
        }

        p.setExperience(p.getExperience() + request.getAmount());
        while (p.getExperience() >= p.getXpThreshold() && p.getLevel() < maxLevel) {
            p.setExperience(p.getExperience() - p.getXpThreshold());
            levelUp(p);
        }
        return playerRepository.save(p);
    }

    private void levelUp(Player p) {
        p.setLevel(p.getLevel() + 1);
        p.setMaxMonsters(p.getMaxMonsters() + slotsPerLevel);
        p.setXpThreshold((int) Math.round(p.getXpThreshold() * xpMultiplier));
    }

    public Player addMonster(String username, AddMonsterRequest request) {
        Player p = getOrCreatePlayer(username);
        if (p.getMonsters().size() >= p.getMaxMonsters()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Capacité maximale de monstres atteinte (" + p.getMaxMonsters() + ")");
        }
        if (request.getMonsterId() == null || request.getMonsterId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ID du monstre est requis");
        }
        p.getMonsters().add(request.getMonsterId());
        return playerRepository.save(p);
    }

    public Player removeMonster(String username, String monsterId) {
        Player p = getOrCreatePlayer(username);
        if (!p.getMonsters().remove(monsterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Monstre " + monsterId + " non trouvé dans votre liste");
        }
        return playerRepository.save(p);
    }
}
