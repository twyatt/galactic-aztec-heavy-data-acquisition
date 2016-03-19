package edu.sdsu.rocket.core.models;

import edu.sdsu.rocket.core.helpers.AtomicFloat;

public class Analog {

    public final int count;
    private final AtomicFloat a[];

    public Analog(int count) {
        this.count = count;
        this.a = new AtomicFloat[count];
        for (int i = 0; i < a.length; i++) {
            a[i] = new AtomicFloat();
        }
    }

    public void set(int i, float value) {
        a[i].set(value);
    }

    public float get(int i) {
        return a[i].get();
    }

    @Override
    public String toString() {
        if (a.length == 0) {
            return super.toString();
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(super.toString()).append(": ");
        builder.append("A0=").append(a[0].get());
        for (int i = 1; i < a.length; i++) {
            final float value = a[i].get();
            builder.append(", ").append("A").append(i).append("=").append(value);
        }
        return builder.toString();
    }
    
}
