package isa.jutjub.service;

import org.springframework.stereotype.Service;

@Service
public class PlaybackService {

    private boolean playing = false;
    private double position = 0;
    private long lastUpdate = System.currentTimeMillis();

    public synchronized void play() {
        playing = true;
        lastUpdate = System.currentTimeMillis();
    }

    public synchronized void pause(double currentPosition) {
        playing = false;
        position = currentPosition;
    }

    public synchronized double getCurrentPosition() {
        if (!playing) return position;

        long now = System.currentTimeMillis();
        return position + (now - lastUpdate) / 1000.0;
    }

    public synchronized void seek(double newPosition) {
        position = newPosition;
        lastUpdate = System.currentTimeMillis();
    }

    public boolean isPlaying() {
        return playing;
    }
}
