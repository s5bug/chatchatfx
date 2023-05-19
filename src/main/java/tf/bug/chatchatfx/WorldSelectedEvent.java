package tf.bug.chatchatfx;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import tf.bug.chatchatfx.model.RoomEntryModel;

public class WorldSelectedEvent extends Event {

    public static final EventType<WorldSelectedEvent> WORLD_SELECTED_EVENT =
            new EventType<WorldSelectedEvent>("WORLD_SELECTED_EVENT");

    private final RoomEntryModel room;

    public WorldSelectedEvent(RoomEntryModel room) {
        super(WORLD_SELECTED_EVENT);
        this.room = room;
    }

    public WorldSelectedEvent(RoomEntryModel room, Object source, EventTarget target) {
        super(source, target, WORLD_SELECTED_EVENT);
        this.room = room;
    }

    public RoomEntryModel getRoom() {
        return this.room;
    }

}
