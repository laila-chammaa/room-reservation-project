package Replicas.Replica1.model;

import java.io.Serializable;
import java.util.Objects;

public class Timeslot implements Serializable {
    private float start;
    private float end;

    public Timeslot() {
    }


    public Timeslot(float start, float end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timeslot timeslot = (Timeslot) o;
        return Float.compare(timeslot.start, start) == 0 &&
                Float.compare(timeslot.end, end) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "Timeslot{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
