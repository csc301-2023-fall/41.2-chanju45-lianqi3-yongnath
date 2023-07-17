package com.snowtheghost.redistributor.api.models.requests.games;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import com.snowtheghost.redistributor.database.models.Game;
import lombok.Setter;

@Getter
@Setter
public class  CreateGameRequest {

    @JsonProperty
    private int capacity;

    @JsonProperty
    private int cost;

    @JsonProperty
    private Game.Type type;
}
