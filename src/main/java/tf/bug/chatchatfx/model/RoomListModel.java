package tf.bug.chatchatfx.model;

import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class RoomListModel {

    private final ListProperty<RoomEntryModel> rooms;

    public RoomListModel() {
        this(List.of());
    }

    public RoomListModel(List<RoomEntryModel> rooms) {
        this.rooms = new SimpleListProperty<>(this, "rooms", FXCollections.observableArrayList(rooms));
    }

    public ListProperty<RoomEntryModel> roomsProperty() {
        return this.rooms;
    }

}
