import ballerina/http;
import ballerina/openapi;

listener http:Listener ep0 = new(80, config = {host: "petstore.openapi.io"});

listener http:Listener ep1 = new(443, config = {host: "petstore.swagger.io"});

@openapi:ServiceInfo {
    contract: "../../swagger/invalid/petstore_path_parameter.yaml",
    tags: [ ]
}
@http:ServiceConfig {
    basePath: "/v1"
}
service petstore on ep0, ep1 {
    @http:ResourceConfig {
        methods:["GET"],
        path:"/pets"
    }
    resource function listPets (http:Caller caller, http:Request req) returns error? {

    }

    @http:ResourceConfig {
        methods:["POST"],
        path:"/pets"
    }
    resource function resource_post_pets (http:Caller caller, http:Request req) returns error? {

    }
    // extra path
    @http:ResourceConfig {
        methods:["POST"],
        path:"/extraPathPet"
    }
    resource function resource_post_pet (http:Caller caller, http:Request req) returns error? {

    }

    @http:ResourceConfig {
        methods:["GET"],
        path:"/pets/{petId}"
    }
    resource function showPetById (http:Caller caller, http:Request req,  string petId) returns error? {

    }

}