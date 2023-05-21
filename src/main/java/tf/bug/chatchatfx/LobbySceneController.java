package tf.bug.chatchatfx;

import com.almasb.fxgl.app.FXGLPane;
import com.almasb.fxgl.app.GameApplication;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tf.bug.chatchat.JRooms;
import tf.bug.chatchatfx.control.RoomList;
import tf.bug.chatchatfx.game.WorldPlayGameApplication;
import tf.bug.chatchatfx.model.RoomEntryModel;
import tf.bug.chatchatfx.model.RoomListModel;

public class LobbySceneController extends VBox implements Initializable {
    @FXML
    private RoomList roomList;
    @FXML
    private Button refreshButton;
    @FXML
    private ProgressBar autoRefreshProgressBar;
    @FXML
    private TextField playerName;

    private AnimationTimer autoRefreshTimer;

    private final RoomListModel rlm;

    private final HttpClient httpClient;
    private final HttpRequest roomsRequest;

    public LobbySceneController(HttpClient httpClient) {
        this.rlm = new RoomListModel();
        this.httpClient = httpClient;
        this.roomsRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://chatchat.nfshost.com/"))
                .header("Origin", "https://chatchatgame.netlify.app")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                .GET()
                .build();

        FXMLLoader fxmlLoader = new FXMLLoader(LobbySceneController.class.getResource("lobby-scene-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.roomList.initialize(rlm);
        this.roomList.addEventHandler(WorldSelectedEvent.WORLD_SELECTED_EVENT, (WorldSelectedEvent we) -> {
            RoomEntryModel rem = we.getRoom();
            WorldPlayGameApplication app = new WorldPlayGameApplication(rem, this.httpClient, this.playerName.getText());
            FXGLPane gamePane = GameApplication.embeddedLaunch(app);
            Scene scene = new Scene(gamePane);
            Stage stage = new Stage();
            stage.setTitle("ChatChat: %s".formatted(rem.getRoomName()));
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(event -> {
                app.close();
            });
            we.consume();
        });

        this.refresh();

        this.refreshButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent me) -> {
            this.refresh();
        });

        final long period = Duration.ofSeconds(10).toNanos();
        this.autoRefreshTimer = new AnimationTimer() {

            long lastRefresh = 0;

            boolean justStarted = false;

            @Override
            public void start() {
                super.start();
                this.justStarted = true;
            }

            @Override
            public void handle(long now) {
                if(this.justStarted) {
                    this.lastRefresh = now;
                    this.justStarted = false;
                    return;
                }

                long elapsed = (now - lastRefresh);
                double progress = ((double) elapsed) / ((double) period);

                LobbySceneController.this.autoRefreshProgressBar.setProgress(progress);

                if(now - this.lastRefresh > period) {
                    this.stop();
                    LobbySceneController.this.refresh().whenComplete((v, t) -> {
                        this.start();
                    });
                }
            }

            @Override
            public void stop() {
                super.stop();
                LobbySceneController.this.autoRefreshProgressBar.setProgress(0.0);
            }
        };

        this.autoRefreshTimer.start();
    }

    private CompletableFuture<Void> refresh() {
        return this.httpClient.sendAsync(this.roomsRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenCompose(hr -> {
                    JsonParser jp = JsonProvider.provider().createParser(hr.body());
                    jp.next();
                    JRooms jr = JRooms.decode(jp.getValue());
                    final List<RoomEntryModel> rems =
                            jr.rooms().stream()
                                    .map(room -> new RoomEntryModel(room.id(), room.name(), room.cats(), room.hasPassword()))
                                    .toList();

                    CompletableFuture<Void> setComplete = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        this.rlm.roomsProperty().setAll(rems);
                        setComplete.complete(null);
                    });
                    return setComplete;
                }).exceptionally(t -> { t.printStackTrace(); return null; });
    }
}
