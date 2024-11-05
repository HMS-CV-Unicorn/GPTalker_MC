package saratoga.gptalkerMC;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class GptalkerMC extends JavaPlugin implements Listener {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onEnable() {
        getLogger().info("GptalkerMC has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("GptalkerMC has been disabled!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerMessage = event.getMessage();
        event.getPlayer().sendMessage("Thinking...");

        // Asynchronous request to LLM API
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String response = fetchLLMResponse(playerMessage);
            if (response != null) {
                getServer().getScheduler().runTask(this, () -> {
                    event.getPlayer().sendMessage(response);
                });
            }
        });
    }

    private String fetchLLMResponse(String message) {
        try {
            RequestBody body = RequestBody.create(
                    message, okhttp3.MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("http://localhost:11434/ollama/chat") // LLM endpoint
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            getLogger().severe("Error connecting to LLM: " + e.getMessage());
            e.printStackTrace();
            return "Error connecting to LLM.";
        }
    }
}