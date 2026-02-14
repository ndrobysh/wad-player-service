package com.wad.player.controller;

import com.wad.player.dto.AddMonsterRequest;
import com.wad.player.dto.GainExperienceRequest;
import com.wad.player.model.Player;
import com.wad.player.service.AuthService;
import com.wad.player.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "API de gestion des joueurs")
public class PlayerController {

    private final PlayerService playerService;
    private final AuthService authService;

    public PlayerController(PlayerService playerService, AuthService authService) {
        this.playerService = playerService;
        this.authService = authService;
    }

    @Operation(summary = "Profil via token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré"),
            @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    @GetMapping("/profile")
    public ResponseEntity<Player> getProfile(@RequestHeader("Authorization") String token) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(playerService.getProfile(username));
    }

    @Operation(summary = "Liste IDs monstres du joueur")
    @GetMapping("/monsters")
    public ResponseEntity<List<String>> getMonsterIds(@RequestHeader("Authorization") String token) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(playerService.getMonsterIds(username));
    }

    @Operation(summary = "Niveau du joueur")
    @GetMapping("/level")
    public ResponseEntity<Map<String, Object>> getLevel(@RequestHeader("Authorization") String token) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("username", username, "level", playerService.getLevel(username)));
    }

    @Operation(summary = "Profil par username", description = "Route utilisée par le frontend")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré"),
            @ApiResponse(responseCode = "403", description = "Accès interdit")
    })
    @GetMapping("/{username}")
    public ResponseEntity<Player> getPlayerByUsername(
            @RequestHeader("Authorization") String token,
            @PathVariable String username) {
        String authenticatedUser = authService.validateToken(token);
        if (!authenticatedUser.equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(playerService.getProfile(username));
    }

    @Operation(summary = "Gain d'XP")
    @PostMapping("/experience")
    public ResponseEntity<Player> gainExperience(@RequestHeader("Authorization") String token,
                                                  @RequestBody GainExperienceRequest request) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(playerService.gainExperience(username, request));
    }

    @Operation(summary = "Ajouter un monstre")
    @PostMapping("/monsters")
    public ResponseEntity<Player> addMonster(@RequestHeader("Authorization") String token,
                                              @RequestBody AddMonsterRequest request) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(playerService.addMonster(username, request));
    }

    @Operation(summary = "Supprimer un monstre")
    @DeleteMapping("/monsters/{monsterId}")
    public ResponseEntity<Player> removeMonster(@RequestHeader("Authorization") String token,
                                                 @PathVariable String monsterId) {
        String username = authService.validateToken(token);
        return ResponseEntity.ok(playerService.removeMonster(username, monsterId));
    }
}
