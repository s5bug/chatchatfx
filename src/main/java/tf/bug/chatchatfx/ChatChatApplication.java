package tf.bug.chatchatfx;

import java.net.http.HttpClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatChatApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        LobbySceneController lsc = new LobbySceneController(httpClient);
        Scene scene = new Scene(lsc, 640, 480);
        stage.setTitle("ChatChat");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
