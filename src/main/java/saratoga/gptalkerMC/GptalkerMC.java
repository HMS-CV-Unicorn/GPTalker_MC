package saratoga.gptalkerMC;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GptalkerMC extends JavaPlugin implements Listener {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Map<UUID, List<Message>> conversationHistory = new HashMap<>();
    private String modelName = "hf.co/team-hatakeyama-phase2/Tanuki-8x8B-dpo-v1.0-GGUF:Q2_K_S";  // モデル名を設定
    private String systemPrompt = "Hello! How can I assist you today?";  // システムプロンプト

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();
        player.sendMessage("考え中...");

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String response = fetchLLMResponse(player.getUniqueId(), playerMessage);
            if (response != null) {
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage(response);
                });
            }
        });
    }

    private String fetchLLMResponse(UUID playerId, String message) {
        try {
            List<Message> history = conversationHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
            if (history.isEmpty()) {
                history.add(new Message("system", systemPrompt));
            }
            history.add(new Message("user", message));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", modelName);
            requestMap.put("messages", history);

            String jsonBody = gson.toJson(requestMap);

            RequestBody body = RequestBody.create(
                jsonBody, okhttp3.MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("http://localhost:11434/api/chat")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.body() != null ? response.body().string() : "Error: No response body";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error connecting to LLM.";
        }
    }

    private static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
