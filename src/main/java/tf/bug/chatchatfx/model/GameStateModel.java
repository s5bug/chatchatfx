package tf.bug.chatchatfx.model;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class GameStateModel {

    private final MapProperty<Integer, PlayerModel> players;
    private final MapProperty<Integer, String> names;
    private final IntegerProperty meId;
    private final ListProperty<ChatMessageModel> chatMessages;

    private final IntegerProperty mouseX;
    private final IntegerProperty mouseY;

    public GameStateModel() {
        this.players = new SimpleMapProperty<>(this, "players", FXCollections.observableHashMap());
        this.names = new SimpleMapProperty<>(this, "names", FXCollections.observableHashMap());
        this.meId = new SimpleIntegerProperty(this, "meId");
        this.chatMessages = new SimpleListProperty<>(this, "chatMessages", FXCollections.observableArrayList());

        this.mouseX = new SimpleIntegerProperty(this, "mouseX");
        this.mouseY = new SimpleIntegerProperty(this, "mouseY");
    }

    public void handleBytes(ByteBuffer from) {
        while (from.hasRemaining()) {
            PlayerModel.deserialize(this.players, from);
        }
    }

    public void handleText(String tpe, String data) {
        switch (tpe) {
            case "id" -> {
                // TODO delete old me from map
                int meId = Integer.parseInt(data);
                this.meId.set(meId);
            }
            case "names" -> {
                JsonParser jp = JsonProvider.provider().createParser(new StringReader(data));
                jp.next();
                JsonObject jo = jp.getObject();
                jo.forEach((String k, JsonValue jv) -> {
                    int id = Integer.parseInt(k);
                    String name = ((JsonString) jv).getString();
                    this.names.put(id, name);
                });
            }
            case "mouse" -> {
                JsonParser jp = JsonProvider.provider().createParser(new StringReader(data));
                jp.next();
                JsonArray ja = jp.getArray();
                this.mouseX.set(ja.getInt(0));
                this.mouseY.set(ja.getInt(1));
            }
            case "hasmouse" -> {}
            case "dropoff" -> {}
            case "invalid" -> {}
            case "frozen" -> {}
            // TODO non-player messages
            default -> {}
        }
    }

    public ObservableMap<Integer, PlayerModel> getPlayers() {
        return players.get();
    }

    public MapProperty<Integer, PlayerModel> playersProperty() {
        return players;
    }

    public ObservableMap<Integer, String> getNames() {
        return names.get();
    }

    public MapProperty<Integer, String> namesProperty() {
        return names;
    }

    public int getMeId() {
        return meId.get();
    }

    public IntegerProperty meIdProperty() {
        return meId;
    }

    public ObservableList<ChatMessageModel> getChatMessages() {
        return chatMessages.get();
    }

    public ListProperty<ChatMessageModel> chatMessagesProperty() {
        return chatMessages;
    }

    public int getMouseX() {
        return mouseX.get();
    }

    public IntegerProperty mouseXProperty() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY.get();
    }

    public IntegerProperty mouseYProperty() {
        return mouseY;
    }

}
