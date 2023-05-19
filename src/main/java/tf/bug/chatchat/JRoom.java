package tf.bug.chatchat;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;

public record JRoom(int id, String name, int cats, boolean hasPassword) {

    public static JRoom decode(JsonValue j) {
        if(!(j instanceof JsonObject jo)) {
            throw new IllegalArgumentException("Expected JsonObject, got: %s".formatted(j));
        }

        int id = jo.getInt("id");
        String name = jo.getString("name");
        int cats = jo.getInt("cats");
        boolean hasPassword = jo.getBoolean("hasPassword");
        return new JRoom(id, name, cats, hasPassword);
    }

}
