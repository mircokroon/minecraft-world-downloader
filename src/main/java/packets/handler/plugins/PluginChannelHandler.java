package packets.handler.plugins;

import config.Config;
import config.Option;
import config.Version;
import packets.DataTypeProvider;

public abstract class PluginChannelHandler {

    private static PluginChannelHandler instance;

    public static PluginChannelHandler getInstance() {
        if (instance == null) {
            instance = Config.versionReporter().select(PluginChannelHandler.class,
                   Option.of(Version.V1_12, PluginChannelHandler1_12::new),
                   Option.of(Version.ANY, DefaultPluginChannelHandler::new)
            );
        }
        return instance;
    }

    public abstract void handleCustomPayload(DataTypeProvider provider);
}

class DefaultPluginChannelHandler extends PluginChannelHandler {
    @Override
    public void handleCustomPayload(DataTypeProvider provider) { }
}
