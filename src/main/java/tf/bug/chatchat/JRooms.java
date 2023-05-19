package tf.bug.chatchat;

import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import java.util.List;

public record JRooms(List<JRoom> rooms) {

    public static JRooms decode(JsonValue o) {
        if(!(o instanceof JsonArray ja)) {
            throw new IllegalArgumentException("Expected JsonArray, got: %s".formatted(o));
        }

        List<JRoom> rooms = ja.stream().map(JRoom::decode).toList();
        return new JRooms(rooms);
    }

}
