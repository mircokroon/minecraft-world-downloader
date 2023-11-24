package game.protocol;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtocolVersionHandlerTest {

    @Test
    void bestMatch() {
        ProtocolVersionHandler pvh = ProtocolVersionHandler.getInstance();

        Map<Integer, String> versions = new HashMap<>();
        versions.put(340, "1.12.2");
        versions.put(404, "1.13.2");
        versions.put(498, "1.14.4");
        versions.put(578, "1.15.2");
        versions.put(754, "1.16.2");
        versions.put(755, "1.17");
        versions.put(756, "1.17");
        versions.put(757, "1.18");
        versions.put(758, "1.18");
        versions.put(761, "1.19.3");
        versions.put(763, "1.20");
        versions.put(764, "1.20.2");

        versions.forEach((k, v) -> {
            assertThat(pvh.getProtocolByProtocolVersion(k).getVersion()).isEqualTo(v);
        });
    }
}