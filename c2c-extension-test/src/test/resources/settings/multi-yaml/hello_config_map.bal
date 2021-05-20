import ballerina/io;
import ballerina/http;
import ballerina/log;

listener http:Listener helloWorldEP = new(9090, {
    secureSocket: {
        key: {
            certFile: "./resource/public.crt",
            keyFile: "./resource/private.key"
        }
    }
});

configurable string users = ?;
configurable string groups = ?;

service /helloWorld on helloWorldEP {
    resource function get config(http:Caller caller, http:Request request) returns @tainted error? {
        http:Response response = new;
        string payload = "Configuration: " + users + " " + groups;
        response.setTextPayload(payload + "\n");
        var responseResult = caller->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", 'error = responseResult);
        }
    }

    resource function get data(http:Caller caller, http:Request request) {
        http:Response response = new;
        string payload = <@untainted> readFile("./data/data.txt");
        response.setTextPayload("Data: " + <@untainted> payload + "\n");
        var responseResult = caller->respond(response);
        if (responseResult is error) {
            log:printError("error responding back to client.", 'error = responseResult);
        }
    }
}

function readFile(string filePath) returns @tainted string {
    io:ReadableByteChannel bchannel = checkpanic io:openReadableFile(filePath);
    io:ReadableCharacterChannel cChannel = new io:ReadableCharacterChannel(bchannel, "UTF-8");

    var readOutput = cChannel.read(50);
    if (readOutput is string) {
        return readOutput;
    } else {
        return "Error: Unable to read file";
    }
}
