package com.zerodha.jpdfsigner;

import com.google.gson.annotations.SerializedName;

// Coordinates class to represent the signature coordinates
class Coordinates {
    @SerializedName("x1")
    private Float x1;

    @SerializedName("y1")
    private Float y1;

    @SerializedName("x2")
    private Float x2;

    @SerializedName("y2")
    private Float y2;

    public Coordinates() {
    }

    public Float getX1() {
        return x1;
    }

    public Float getY1() {
        return y1;
    }

    public Float getX2() {
        return x2;
    }

    public Float getY2() {
        return y2;
    }

    public boolean isValid() {
        return x1 != null && y1 != null && x2 != null && y2 != null;
    }
}

// Request is the class that represents the json request body of the API.
class Request {
    @SerializedName("reason")
    private String reason;

    @SerializedName("contact")
    private String contact;

    @SerializedName("location")
    private String location;

    @SerializedName("input_file")
    private String inputFile;

    @SerializedName("output_file")
    private String outputFile;

    @SerializedName("password")
    private String password;

    @SerializedName("page")
    private Integer page;

    @SerializedName("coordinates")
    private Coordinates coordinates;

    public Request() {
    }

    public String getReason() {
        return reason;
    }

    public String getContact() {
        return contact;
    }

    public String getLocation() {
        return location;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String getPassword() {
        return password;
    }

    public Integer getPage() {
        return page;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
}
