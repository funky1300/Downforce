package com.example.downforce;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Race {
    public String name;
    public String location;
    public String flag;
    public String circuit;
    public ZonedDateTime date;

    public Race(String name, String location, String dateString, String circuit, String flag) {
        this.name = name;
        this.location = location;
        this.flag = flag;
        this.circuit = circuit;
        
        // OpenF1 API often returns dates like "2024-03-02T15:00:00" without timezone.
        // ZonedDateTime.parse requires a timezone offset (like "Z" or "+00:00").
        if (dateString != null && !dateString.isEmpty() && !dateString.contains("Z") && !dateString.contains("+")) {
            dateString += "Z";
        }
        
        try {
            this.date = ZonedDateTime.parse(dateString);
        } catch (Exception e) {
            // Fallback to now if parsing fails
            this.date = ZonedDateTime.now();
        }
    }

    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getFlag() { return flag; }
    public String getCircuit() { return circuit; }

    public String getDate() {
        if (date == null) return "Unknown Date";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return date.format(formatter);
    }

    public int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) return Color.TRANSPARENT;
        Bitmap tiny = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int averageColor = tiny.getPixel(0, 0);
        tiny.recycle();
        return averageColor;
    }
}
