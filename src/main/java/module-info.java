module tf.bug.chatchatfx {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;
    requires com.tobiasdiez.easybind;
    requires jakarta.json;
    requires java.net.http;

    opens tf.bug.chatchatfx to javafx.fxml;
    exports tf.bug.chatchatfx;
    exports tf.bug.chatchatfx.model;
    opens tf.bug.chatchatfx.model to javafx.fxml;
    exports tf.bug.chatchatfx.control;
    opens tf.bug.chatchatfx.control to javafx.fxml;
    opens tf.bug.chatchatfx.game to javafx.fxml;
    exports tf.bug.chatchatfx.game;
}
