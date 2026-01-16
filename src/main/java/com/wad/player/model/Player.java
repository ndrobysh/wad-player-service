package com.wad.player.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "players")
public class Player {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private int level;

    private int experience;
}
