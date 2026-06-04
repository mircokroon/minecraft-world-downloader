package packets.builder;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

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

    public SpecificTag toNbt() {
        // A proper NBT text component compound so styling (colour/bold/children) survives on 1.20.3+,
        // where chat content is sent as NBT rather than a JSON string.
        CompoundTag tag = new CompoundTag();
        tag.add("text", new StringTag(text == null ? "" : text));
        if (color != null) {
            tag.add("color", new StringTag(color));
        }
        if (bold) {
            tag.add("bold", new ByteTag(1));
        }
        if (extra != null && !extra.isEmpty()) {
            List<SpecificTag> children = new ArrayList<>();
            for (Chat c : extra) {
                children.add(c.toNbt());
            }
            tag.add("extra", new ListTag(Tag.TAG_COMPOUND, children));
        }
        return tag;
    }
}
