package com.example.downforce;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;



public class Race {
    public String name;
    public String location;
    public String flag;
    public ZonedDateTime date;

    public Race(String name, String location, String dateString, String flag) {
        this.name = name;
        this.location = location;
        this.flag = flag;
        this.date = ZonedDateTime.parse(dateString);
    }

    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getFlag() { return flag; }

    public String getDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return date.format(formatter);
    }



    public int getAverageColor(Bitmap bitmap) {
        if (bitmap == null) return Color.TRANSPARENT;

        Bitmap tiny = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int averageColor = tiny.getPixel(0, 0);

        tiny.recycle(); // Clean up the 1x1 bitmap
        return averageColor;
    }
}