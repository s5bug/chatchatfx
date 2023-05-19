package tf.bug.chatchatfx.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class PlayerModel {

    private final IntegerProperty x;
    private final IntegerProperty y;
    private final IntegerProperty color;
    private final Property<Facing> facing;
    private final BooleanProperty isNapping;
    private final BooleanProperty isDog;

    public PlayerModel() {
        this.x = new SimpleIntegerProperty(this, "x");
        this.y = new SimpleIntegerProperty(this, "y");
        this.color = new SimpleIntegerProperty(this, "color");
        this.facing = new SimpleObjectProperty<>(this, "facing");
        this.isNapping = new SimpleBooleanProperty(this, "isNapping");
        this.isDog = new SimpleBooleanProperty(this, "isDog");
    }

    public PlayerModel(
            int x,
            int y,
            int color,
            Facing facing,
            boolean isNapping,
            boolean isDog
    ) {
        this.x = new SimpleIntegerProperty(this, "x", x);
        this.y = new SimpleIntegerProperty(this, "y", y);
        this.color = new SimpleIntegerProperty(this, "color", color);
        this.facing = new SimpleObjectProperty<>(this, "facing", facing);
        this.isNapping = new SimpleBooleanProperty(this, "isNapping", isNapping);
        this.isDog = new SimpleBooleanProperty(this, "isDog", isDog);
    }

    public void serialize(int id, ByteBuffer into) {
        ByteOrder prev = into.order();
        into.order(ByteOrder.BIG_ENDIAN);

        int data = (id << 24) +
                (this.x.get() << 16) +
                (this.y.get() << 8) +
                (this.color.get() << 4) +
                ((this.isDog.get() ? 1 : 0) << 3) +
                ((this.isNapping.get() ? 1 : 0) << 2) +
                (this.facing.getValue().ordinal() << (0));

        into.putInt(data);

        into.order(prev);
    }

    public static void deserialize(Map<Integer, PlayerModel> into, ByteBuffer from) {
        ByteOrder prev = from.order();
        from.order(ByteOrder.BIG_ENDIAN);

        int data = from.getInt();

        int id = (data >>> 24) & 0xFF;
        boolean erased = ((data) & ~(0xFF << 24)) == 0;

        if(erased) {
            into.remove(id);
        } else {
            int x = (data >>> 16) & 0xFF;
            int y = (data >>> 8) & 0xFF;
            int color = (data >>> 4) & 0x0F;
            boolean isDog = ((data >>> 3) & 0x01) != 0;
            boolean isNapping = ((data >>> 2) & 0x01) != 0;
            Facing facing = Facing.values()[(data >>> (0)) & 0x03];

            PlayerModel toUpdate = into.computeIfAbsent(id, _id -> new PlayerModel());
            toUpdate.xProperty().set(x);
            toUpdate.yProperty().set(y);
            toUpdate.colorProperty().set(color);
            toUpdate.isDogProperty().set(isDog);
            toUpdate.isNappingProperty().set(isNapping);
            toUpdate.facingProperty().setValue(facing);
        }

        from.order(prev);
    }

    public int getX() {
        return this.x.get();
    }

    public IntegerProperty xProperty() {
        return this.x;
    }

    public int getY() {
        return this.y.get();
    }

    public IntegerProperty yProperty() {
        return this.y;
    }

    public int getColor() {
        return this.color.get();
    }

    public IntegerProperty colorProperty() {
        return this.color;
    }

    public Facing getFacing() {
        return this.facing.getValue();
    }

    public Property<Facing> facingProperty() {
        return this.facing;
    }

    public boolean getIsNapping() {
        return this.isNapping.get();
    }

    public BooleanProperty isNappingProperty() {
        return this.isNapping;
    }

    public boolean getIsDog() {
        return this.isDog.get();
    }

    public BooleanProperty isDogProperty() {
        return this.isDog;
    }
}
