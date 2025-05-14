package com.pequla.dasher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscordModel {
    private String id;
    private String name;
    private String nickname;
    private String avatar;
}
