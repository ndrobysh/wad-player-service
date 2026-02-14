package com.wad.player.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;          // frontend attend "username"

    private int level;
    private int experience;
    private int xpThreshold;          // frontend attend "xpThreshold"
    private int maxMonsters;          // frontend attend "maxMonsters"
    private List<String> monsters = new ArrayList<>();  // frontend attend "monsters"
}
