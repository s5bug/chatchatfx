package tf.bug.chatchatfx.control;

import com.tobiasdiez.easybind.EasyBind;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.VBox;
import tf.bug.chatchatfx.WorldSelectedEvent;
import tf.bug.chatchatfx.model.RoomEntryModel;
import tf.bug.chatchatfx.model.RoomListModel;

public class RoomList extends VBox {
    private final AtomicReference<RoomListModel> model;

    @FXML
    private TableView<RoomEntryModel> roomTableView;

    public RoomList() {
        FXMLLoader fxmlLoader = new FXMLLoader(RoomList.class.getResource("room-list-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TableColumn<RoomEntryModel, Number> roomIdColumn = new TableColumn<>("Id");
        roomIdColumn.setCellValueFactory(d -> d.getValue().roomIdProperty());

        TableColumn<RoomEntryModel, String> roomNameColumn = new TableColumn<>("Room Name");
        roomNameColumn.setCellValueFactory(d -> d.getValue().roomNameProperty());

        TableColumn<RoomEntryModel, Number> playerCountColumn = new TableColumn<>("Players");
        playerCountColumn.setCellValueFactory(d -> d.getValue().playerCountProperty());

        TableColumn<RoomEntryModel, Boolean> isLockedColumn = new TableColumn<>("Password");
        isLockedColumn.setCellValueFactory(d -> d.getValue().isLockedProperty());

        TableColumn<RoomEntryModel, RoomEntryModel> connectButtonColumn = new TableColumn<>();
        connectButtonColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        connectButtonColumn.setCellFactory(d -> {
            Button connect = new Button("Connect");
            TableCell<RoomEntryModel, RoomEntryModel> cell = new TableCell<>() {
                @Override
                protected void updateItem(RoomEntryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty) this.setGraphic(null);
                    else this.setGraphic(connect);
                }
            };
            connect.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent ev) -> {
                this.fireEvent(new WorldSelectedEvent(cell.itemProperty().get(), connect, RoomList.this));
            });
            return cell;
        });
        connectButtonColumn.setSortable(false);

        this.roomTableView.getColumns().setAll(roomIdColumn, roomNameColumn, playerCountColumn, isLockedColumn, connectButtonColumn);
        this.roomTableView.getSortOrder().add(roomIdColumn);

        this.model = new AtomicReference<>(null);
    }

    public RoomList(RoomListModel model) {
        this();
        this.initialize(model);
    }

    public void initialize(RoomListModel model) {
        if(!this.model.compareAndSet(null, model)) {
            throw new IllegalStateException("Attempted to initialize model multiple times");
        }

        ObservableValue<SortedList<RoomEntryModel>> sortByRoomId =
                model.roomsProperty().map(l -> new SortedList<>(l, Comparator.comparing(RoomEntryModel::getRoomId)));
        sortByRoomId.addListener((observable, oldValue, newValue) -> newValue.comparatorProperty().bind(this.roomTableView.comparatorProperty()));
        this.roomTableView.itemsProperty().bind(sortByRoomId);
    }

}
