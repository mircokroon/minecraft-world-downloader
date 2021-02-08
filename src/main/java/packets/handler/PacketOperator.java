package packets.handler;

import packets.DataTypeProvider;

import java.util.function.Function;

public interface PacketOperator extends Function<DataTypeProvider, Boolean> {
}
