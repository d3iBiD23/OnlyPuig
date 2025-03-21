package com.example.onlypuig;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class TimeUtils {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getRelativeTime(String createdAt) {
        try {
            Instant created = Instant.parse(createdAt);
            Duration duration = Duration.between(created, Instant.now());
            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return "Hace " + seconds + " seg";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return "Hace " + minutes + " min";
            } else if (seconds < 86400) {
                long hours = seconds / 3600;
                return "Hace " + hours + " h";
            } else {
                long days = seconds / 86400;
                return "Hace " + days + (days > 1 ? " dias" : " dia");
            }
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return createdAt;
        }
    }
}
