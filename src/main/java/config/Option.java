package config;

import java.util.function.Supplier;

public class Option {
    Version v;
    Supplier<Object> obj;

    public Option(Version v, Supplier<Object> obj) {
        this.v = v;
        this.obj = obj;
    }

    public static Option of(Version v, Supplier<Object> obj) {
        return new Option(v, obj);
    }
}
