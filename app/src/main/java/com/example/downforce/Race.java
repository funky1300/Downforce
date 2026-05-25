package com.example.downforce;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Race {
    public int id;
    public String name;
    public String location;
    public String flag;
    public String circuit;
    public ZonedDateTime startDate;
    public ZonedDateTime endDate;

    public Race(int id, String name, String location, String startDateString, String endDateString, String circuit, String flag) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.flag = flag;
        this.circuit = circuit;
        
        this.startDate = parseDate(startDateString);
        this.endDate = parseDate(endDateString);
    }

    private ZonedDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return ZonedDateTime.now();
        if (!dateString.contains("Z") && !dateString.contains("+")) {
            dateString += "Z";
        }
        try {
            return ZonedDateTime.parse(dateString);
        } catch (Exception e) {
            return ZonedDateTime.now();
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getFlag() { return flag; }
    public String getCircuit() { return circuit; }

    public String getstartDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return startDate.plusHours(1).format(formatter);
    }
    public String getEndDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return endDate.plusHours(1).format(formatter);
    }

    public int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) return Color.TRANSPARENT;
        Bitmap tiny = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int averageColor = tiny.getPixel(0, 0);
        tiny.recycle();
        return averageColor;
    }
}
