package com.example.downforce;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Race — the data model for a single race (a plain POJO, not an Activity).
 *
 * PURPOSE (why): represents one race's data and keeps it separate from the
 * Activities — this is the logic/UI separation (OOP) the bagrut asks for.
 *
 * HOW (how): plain fields + getters. Dates are ZonedDateTime; parseDate() reads
 * the ISO format, and getStartDate()/getEndDate() convert from UTC to the
 * device's local time (handling daylight-saving automatically).
 *
 * Bagrut: OOP / separation of logic and display (req 12).
 */
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

    public String getStartDate() {
        if (startDate == null) return "Unknown Date";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        // Convert UTC to local device time (handles Summer/Winter time automatically)
        return startDate.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);
    }

    public String getEndDate() {
        if (endDate == null) return "Unknown Date";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return endDate.withZoneSameInstant(ZoneId.systemDefault()).format(formatter);
    }

    public String getDate() {
        return getStartDate();
    }

    public int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) return Color.TRANSPARENT;
        Bitmap tiny = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int averageColor = tiny.getPixel(0, 0);
        tiny.recycle();
        return averageColor;
    }
}
