package com.zerodha.jpdfsigner;

import com.google.gson.annotations.SerializedName;

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

}
