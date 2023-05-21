package tf.bug.chatchatfx.game;

import com.almasb.fxgl.app.FXGLApplication;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.MainWindow;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import tf.bug.chatchatfx.model.Facing;
import tf.bug.chatchatfx.model.GameStateModel;
import tf.bug.chatchatfx.model.PlayerModel;
import tf.bug.chatchatfx.model.RoomEntryModel;

public class WorldPlayGameApplication extends GameApplication {

    private final RoomEntryModel rem;
    private final HttpClient httpClient;
    private GameWebsocket webSocket;
    private final String playerName;

    private final GameStateModel gameState;

    private final ConcurrentLinkedDeque<Throwable> errorsToThrow;

    public WorldPlayGameApplication(RoomEntryModel rem, HttpClient hc, String playerName) {
        super();
        this.rem = rem;
        this.httpClient = hc;
        this.playerName = playerName;
        this.gameState = new GameStateModel();

        this.errorsToThrow = new ConcurrentLinkedDeque<>();
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setManualResizeEnabled(true);
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

        Consumer<Throwable> onError = errorsToThrow::add;

        this.webSocket = new GameWebsocket(this.httpClient, wsUri, this.gameState, onError);

        EasyBinding<Optional<PlayerModel>> meOptional = EasyBind.mapObservable(this.gameState.meIdProperty(), mid -> EasyBind.valueAt(this.gameState.playersProperty(), mid.intValue()));
        EasyBinding<PlayerModel> me = EasyBind.map(meOptional, op -> op.orElseGet(PlayerModel::new));
        EasyBinding<Integer> meX = me.mapObservable(PlayerModel::xProperty).map(Number::intValue);
        EasyBinding<Integer> meY = me.mapObservable(PlayerModel::yProperty).map(Number::intValue);

        FXGL.getInput().addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                int hereX = meX.get();
                int hereY = meY.get();
                sendMovementPacket(hereX, hereY, Facing.UP);
            }
        }, KeyCode.UP);
        FXGL.getInput().addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                int hereX = meX.get();
                int hereY = meY.get();
                sendMovementPacket(hereX, hereY, Facing.DOWN);
            }
        }, KeyCode.DOWN);
        FXGL.getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                int hereX = meX.get();
                int hereY = meY.get();
                sendMovementPacket(hereX, hereY, Facing.LEFT);
            }
        }, KeyCode.LEFT);
        FXGL.getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                int hereX = meX.get();
                int hereY = meY.get();
                sendMovementPacket(hereX, hereY, Facing.RIGHT);
            }
        }, KeyCode.RIGHT);

        MapProperty<Integer, Entity> cats = new SimpleMapProperty<>(FXCollections.observableHashMap());
        this.gameState.playersProperty().addListener((MapChangeListener<Integer, PlayerModel>) change -> {
            if(change.wasRemoved()) {
                cats.get(change.getKey()).removeFromWorld();
                cats.remove(change.getKey());
            }
            if(change.wasAdded()) {
                Entity ent = FXGL.entityBuilder()
                        .viewWithBBox("missing-texture")
                        .scale(1.0 / 16.0, 1.0 / 16.0)
                        .build();

                ent.xProperty().bind(EasyBind.map(change.getValueAdded().xProperty(), Number::doubleValue));
                ent.yProperty().bind(EasyBind.map(change.getValueAdded().yProperty(), Number::doubleValue));

                Platform.runLater(() -> FXGL.getGameWorld().addEntity(ent));

                cats.put(change.getKey(), ent);
            }
        });

        double[] zooms = { 0.015625, 0.03125, 0.046875, 0.0625, 0.09375, 0.125, 0.1875, 0.25, 0.325, 0.5, 0.75, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0 };
        final int[] zoomLevel = {7};
        FXGL.getInput().addEventHandler(ScrollEvent.SCROLL, ev -> {
            if(ev.getDeltaX() == 0.0) {
                int oldZoomLevel = zoomLevel[0];
                int newZoomLevel = oldZoomLevel;
                if(ev.getDeltaY() > 0) {
                    newZoomLevel = Math.max(0, Math.min(zooms.length - 1, oldZoomLevel + 1));
                }
                if(ev.getDeltaY() < 0) {
                    newZoomLevel = Math.max(0, Math.min(zooms.length - 1, oldZoomLevel - 1));
                }
                double newZoom = zooms[newZoomLevel] * 16.0d;
                FXGL.getGameScene().getViewport().setZoom(newZoom);
                zoomLevel[0] = newZoomLevel;
            }
        });
        FXGL.getGameScene().getViewport().setZoom(zooms[zoomLevel[0]] * 16.0d);
    }

    private void sendMovementPacket(int hereX, int hereY, Facing direction) {
        ByteBuffer bb = ByteBuffer.allocate(4);

        bb.put((byte) hereX);
        bb.put((byte) hereY);
        bb.put((byte) direction.ordinal());
        bb.put((byte) 0);
        bb.flip();

        this.webSocket.sendBinary(bb, true);
    }

    public void close() {
        this.webSocket.close(WebSocket.NORMAL_CLOSURE, "byebye");
    }

    @Override
    protected void onUpdate(double tpf) {
        while(!this.errorsToThrow.isEmpty()) {
            FXGL.getDialogFactoryService().errorDialog(this.errorsToThrow.poll());
        }
    }

}
