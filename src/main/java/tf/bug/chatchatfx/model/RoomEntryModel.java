package tf.bug.chatchatfx.model;

import javafx.beans.property.*;

public class RoomEntryModel {

    private final IntegerProperty roomId;
    private final StringProperty roomName;
    private final IntegerProperty playerCount;
    private final BooleanProperty isLocked;

    public RoomEntryModel(int roomId, String roomName, int playerCount, boolean isLocked) {
        this.roomId = new SimpleIntegerProperty(this, "roomId", roomId);
        this.roomName = new SimpleStringProperty(this, "roomName", roomName);
        this.playerCount = new SimpleIntegerProperty(this, "playerCount", playerCount);
        this.isLocked = new SimpleBooleanProperty(this, "isLocked", isLocked);
    }

    public IntegerProperty roomIdProperty() {
        return this.roomId;
    }

    public int getRoomId() {
        return this.roomId.get();
    }

    public StringProperty roomNameProperty() {
        return this.roomName;
    }

    public String getRoomName() {
        return this.roomName.get();
    }

    public IntegerProperty playerCountProperty() {
        return this.playerCount;
    }

    public BooleanProperty isLockedProperty() {
        return this.isLocked;
    }

}
