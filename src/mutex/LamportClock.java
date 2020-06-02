package mutex;

public class LamportClock {

    private int c;

    public LamportClock() {
        this.c = 1;
    }

    public LamportClock(int value) {
        this.c = value;
    }

    public int getValue() {
        return c;
    }

    public void tick() {
        c++;
    }

    public void receiveAction(int sentValue) {
        c = Math.max(c, sentValue) + 1;
    }

}
