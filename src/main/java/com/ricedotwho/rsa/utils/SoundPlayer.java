package com.ricedotwho.rsa.utils;

import com.ricedotwho.rsa.RSA;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;

public class SoundPlayer {

    public static void playSound(Path soundFile, float volume) {
        String fileName = soundFile.toString().toLowerCase();

        if (fileName.endsWith(".mp3")) {
            playMP3(soundFile, volume);
        }else {
            RSA.chat("Make sure u typed it right. it needs to have .mp3 at the end");
            throw new IllegalArgumentException();
        }
    }

    private static void playMP3(Path soundFile, float volume) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(soundFile.toFile());
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                Player player = new Player(bis);
                player.play();
            } catch (Exception e) {
                RSA.chat(e.getMessage());
            }
        }).start();
    }
}