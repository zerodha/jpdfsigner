package com.zerodha.jpdfsigner;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Coordinates class to represent the signature coordinates
@Getter
@NoArgsConstructor
class Coordinates {
    @SerializedName("x1")
    private Float x1;

    @SerializedName("y1")
    private Float y1;

    @SerializedName("x2")
    private Float x2;

    @SerializedName("y2")
    private Float y2;

    public boolean isValid() {
        return x1 != null && y1 != null && x2 != null && y2 != null;
    }
}

// Request is the class that represents the json request body of the API.
@Getter
@NoArgsConstructor
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

}
