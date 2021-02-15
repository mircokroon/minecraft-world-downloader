package config;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.spi.StringOptionHandler;

/**
 * Handler for the server argument, informs the GUI whether the -s argument was passed or not. We can't look at the
 * value of -s alone because reading previously stored config data will otherwise cause the settings GUI to be skipped
 * when loading from the config file, even if it should be shown.
 */
public class ServerHandler extends StringOptionHandler {
    public ServerHandler(CmdLineParser parser, OptionDef option, Setter<? super String> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String value = params.getParameter(0);
        if (value != null && value.length() > 0) {
            Config.disableSettingsGui();
        }
        return super.parseArguments(params);
    }
}
