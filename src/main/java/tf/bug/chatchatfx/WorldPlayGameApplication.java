package tf.bug.chatchatfx;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.physics.HitBox;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import tf.bug.chatchatfx.model.GameStateModel;
import tf.bug.chatchatfx.model.PlayerModel;
import tf.bug.chatchatfx.model.RoomEntryModel;

public class WorldPlayGameApplication extends GameApplication {

    private final RoomEntryModel rem;
    private final HttpClient httpClient;
    private CompletableFuture<WebSocket> webSocket;
    private final String playerName;

    private final GameStateModel gameState;

    public WorldPlayGameApplication(RoomEntryModel rem, HttpClient hc, String playerName) {
        super();
        this.rem = rem;
        this.httpClient = hc;
        this.playerName = playerName;
        this.gameState = new GameStateModel();
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("Chat Chat: %s".formatted(this.rem.getRoomName()));
    }

    @Override
    protected void initGame() {
        URI wsUri = URI.create(
                "wss://chatchat.nfshost.com/ws/?roomid=%d&name=%s&pass=".formatted(
                        this.rem.getRoomId(),
                        this.playerName
                )
        );
        this.webSocket = this.httpClient.newWebSocketBuilder()
                .header("Origin", "https://chatchatgame.netlify.app")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                .subprotocols("permessage-deflate", "client_max_window_bits")
                .buildAsync(wsUri, new PlayGameWebsocketListener())
                .exceptionally(t -> { t.printStackTrace(); return null; });

        HashMap<Integer, Entity> playerEnts = new HashMap<>();

        this.gameState.playersProperty().addListener((MapChangeListener.Change<? extends Integer, ? extends PlayerModel> change) -> {
            if(change.wasAdded() != change.wasRemoved()) {
                if(change.wasRemoved()) {
                    playerEnts.get(change.getKey()).removeFromWorld();
                } else {
                    Entity ent = FXGL.entityBuilder()
                            .at(change.getValueAdded().getX() * 10, change.getValueAdded().getY() * 10)
                            .buildAndAttach();

                    playerEnts.put(change.getKey(), ent);
                }
            }
        });

        this.gameState.playersProperty().addListener((MapChangeListener<Integer, PlayerModel>) change -> {
            if(!change.wasRemoved()) {
                EasyBind.combine(change.getValueAdded().xProperty(), change.getValueAdded().yProperty(), (x, y) -> {
                    return new Point2D(x.doubleValue(), y.doubleValue());
                }).addListener((observableValue, oldValue, newValue) -> {
                    playerEnts.get(change.getKey()).setPosition(newValue.multiply(10));
                });
            }
        });
    }

    public void close() {
        this.webSocket.whenComplete((sock, err) -> {
            if(sock != null) {
                sock.sendClose(WebSocket.NORMAL_CLOSURE, "byebye");
            }
        });
    }

    private class PlayGameWebsocketListener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // TODO handle `last`
            CompletableFuture<Void> cf = new CompletableFuture<>();
            Platform.runLater(() -> {
                WorldPlayGameApplication.this.gameState.handleBytes(data);
                cf.complete(null);
            });
            webSocket.request(1);
            return cf;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            // TODO handle `last`
            String copy = String.valueOf(data);
            String[] parts = copy.split(" ", 2);
            Platform.runLater(() -> {
                WorldPlayGameApplication.this.gameState.handleText(parts[0], parts[1]);
            });
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

}
