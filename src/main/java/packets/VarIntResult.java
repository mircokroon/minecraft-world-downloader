package packets;

public class VarIntResult {

    private int value;
    private int bytesRead;
    private boolean complete;

    public VarIntResult() {
        reset();
    }

    public void reset() {
        this.complete = false;
        this.value = 0;
        this.bytesRead = 0;
    }

    public VarIntResult(boolean isComplete, int value, int bytesRead) {
        this.complete = isComplete;
        this.value = value;
        this.bytesRead = bytesRead;
    }

    public void addValue(int value) {
        this.value |= value;
    }

    public void addByteRead() {
        this.bytesRead++;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getResult() {
        return value;
    }

    public int numBytes() {
        return bytesRead;
    }
}
