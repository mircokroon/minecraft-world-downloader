package game.data.entity;

import packets.DataTypeProvider;

public interface IMovableEntity {
    void incrementPosition(int dx, int dy, int dz);
    void readPosition(DataTypeProvider provider);
}
