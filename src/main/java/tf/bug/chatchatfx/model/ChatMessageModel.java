package tf.bug.chatchatfx.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public sealed interface ChatMessageModel permits ChatMessageModel.Shout, ChatMessageModel.Help, ChatMessageModel.Emote, ChatMessageModel.Say {

    public static final record Shout(String content) implements ChatMessageModel {}

    public static final record Help(String content) implements ChatMessageModel {}

    public static final record Emote(int color, String author, String content) implements ChatMessageModel {}

    public static final record Say(int color, String author, String content) implements ChatMessageModel {}

}
