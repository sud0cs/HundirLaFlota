package com.example.hundirlaflota;

import androidx.annotation.Nullable;

public class Casella {
    int x;
    int y;
    public Casella(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null) return false;
        Casella c = (Casella) obj;
        return (this.y == c.y && this.x == c.x);
    }

    @Override
    public int hashCode() {
        return (this.x*100)+this.y;
    }
}