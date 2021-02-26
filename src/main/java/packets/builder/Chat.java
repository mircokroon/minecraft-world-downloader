package packets.builder;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat object, can be sent to the client to display messages.
 */
public class Chat {
    String text;
    boolean bold;
    String color;
    List<Chat> extra;

    public Chat(String text) {
        this.text = text;
    }

    public void setColor(String color) {
        this.color = color;
    }
    public void makeBold() {
        this.bold = true;
    }
    public void addChild(Chat c) {
        if (extra == null) {
            extra = new ArrayList<>();
        }
        extra.add(c);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
