package com.sklinfotech.btcgames.lib;

public class JSONBaseResult {

    public String error;

    // If the request handler throws an exception, the result will look like this:
    // {"status": "error", "message": "invalid", "class": "Exception"}
    String status;
    String message;
}
