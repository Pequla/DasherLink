package com.pequla.dasher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class DasherLink extends JavaPlugin implements Listener {

    private HttpClient client;
    private ObjectMapper mapper;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Register json mapper
        this.mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLoginEvent(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        // Whitelist is on and the player is not whitelisted
        if (getServer().hasWhitelist() && !player.isWhitelisted()) {
            // Player is not whitelisted
            return;
        }

        // Player is banned
        if (getServer().getBannedPlayers().stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()))) {
            return;
        }

        // Checking if player has been verified
        try {
            String url = getConfig().getString("url");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url + player.getUniqueId()))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> json = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (json.statusCode() != 200) {
                // Authentication failed
                ErrorModel error = mapper.readValue(json.body(), ErrorModel.class);
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, error.getMessage());
                return;
            }

            DiscordModel discord = mapper.readValue(json.body(), DiscordModel.class);
            getLogger().info(String.format("%s authenticated as: %s [ID: %s]",
                    ChatColor.stripColor(player.getName()),
                    discord.getNickname(),
                    discord.getId()
            ));
        } catch (Exception e) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, e.getMessage());
        }
    }
}
