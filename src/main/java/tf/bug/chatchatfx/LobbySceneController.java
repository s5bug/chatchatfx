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
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tf.bug.chatchat.JRooms;
import tf.bug.chatchatfx.control.RoomList;
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

        this.refreshButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent me) -> {
            this.refresh();
        });

        final long period = Duration.ofSeconds(10).toNanos();
        this.autoRefreshTimer = new AnimationTimer() {

            long lastCheck = 0;
            long lastRefresh = 0;

            @Override
            public void handle(long now) {
                long remainder = now % period;
                double progress = ((double) remainder) / ((double) period);

                LobbySceneController.this.autoRefreshProgressBar.setProgress(progress);

                if(now < this.lastCheck) {
                    // overflow
                    this.lastRefresh = now;
                }

                if(this.lastRefresh - now > period) {
                    LobbySceneController.this.autoRefreshTimer.stop();
                    LobbySceneController.this.refresh();
                    LobbySceneController.this.autoRefreshProgressBar.setProgress(0.0);
                }

                this.lastCheck = now;
            }
        };

        this.refresh();
    }

    private void refresh() {
        this.httpClient.sendAsync(this.roomsRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(hr -> {
                    JsonParser jp = JsonProvider.provider().createParser(hr.body());
                    jp.next();
                    JRooms jr = JRooms.decode(jp.getValue());
                    final List<RoomEntryModel> rems =
                            jr.rooms().stream()
                                    .map(room -> new RoomEntryModel(room.id(), room.name(), room.cats(), room.hasPassword()))
                                    .toList();
                    Platform.runLater(() -> this.rlm.roomsProperty().setAll(rems));
                    this.autoRefreshTimer.start();
                }).exceptionally(t -> { t.printStackTrace(); return null; });
    }
}
