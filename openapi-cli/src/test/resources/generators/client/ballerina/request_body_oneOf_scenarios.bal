import  ballerina/http;

# refComponent
#
# + clientEp - Connector http endpoint
public client class Client {
    http:Client clientEp;
    public isolated function init(http:ClientConfiguration clientConfig =  {}, string serviceUrl = "http://petstore.openapi.io/v1") returns error? {
        http:Client httpEp = check new (serviceUrl, clientConfig);
        self.clientEp = httpEp;
    }
    # Request Body has nested allOf.
    #
    # + payload - A JSON object containing pet information
    # + return - OK
    remote isolated function postXMLUser(Body payload) returns error? {
        string  path = string `/path01`;
        http:Request request = new;
        json jsonBody = check payload.cloneWithType(json);
        request.setPayload(jsonBody);
         _ = check self.clientEp-> post(path, request, targetType=http:Response);
    }
}
