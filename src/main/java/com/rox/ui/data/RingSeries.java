package com.rox.ui.data;

/** Fixed-capacity ring buffer of float samples; oldest samples are overwritten. */
public final class RingSeries {
    private final float[] samples;
    private int head;
    private int count;

    public RingSeries(int capacity) {
        samples = new float[capacity];
    }

    public void add(float value) {
        samples[head] = value;
        head = (head + 1) % samples.length;
        count = Math.min(count + 1, samples.length);
    }

    public int capacity() {
        return samples.length;
    }

    public int size() {
        return count;
    }

    /** Sample {@code i}, where 0 is the oldest retained sample. */
    public float get(int i) {
        if (i < 0 || i >= count) {
            throw new IndexOutOfBoundsException(i);
        }
        return samples[(head - count + i + samples.length) % samples.length];
    }

    public float latest() {
        return count == 0 ? 0 : get(count - 1);
    }

    public float max() {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, get(i));
        }
        return count == 0 ? 0 : max;
    }

    public float average() {
        float sum = 0;
        for (int i = 0; i < count; i++) {
            sum += get(i);
        }
        return count == 0 ? 0 : sum / count;
    }
}
