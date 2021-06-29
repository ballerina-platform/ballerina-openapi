/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.generators.client;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.generators.BallerinaSchemaGenerator;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.generators.GeneratorUtils.convertOpenAPITypeToBallerina;
import static io.ballerina.generators.GeneratorUtils.extractReferenceType;
import static io.ballerina.generators.GeneratorUtils.getBallerinaMeidaType;
import static io.ballerina.generators.GeneratorUtils.getOneOfUnionType;
import static io.ballerina.generators.GeneratorUtils.getValidName;
import static io.ballerina.generators.GeneratorUtils.isValidSchemaName;

public class FunctionReturnType {
    private OpenAPI openAPI;
    private BallerinaSchemaGenerator ballerinaSchemaGenerator;
    private List<TypeDefinitionNode> typeDefinitionNodeList = new LinkedList<>();


    public FunctionReturnType() {}

    public FunctionReturnType(OpenAPI openAPI, BallerinaSchemaGenerator ballerinaSchemaGenerator,
                              List<TypeDefinitionNode> typeDefinitionNodeList) {

        this.openAPI = openAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Generate Type for datatype that can not bind to the targetType.
     *
     * @param type - data Type.
     * @param typeName - Created datType name.
     * @return return dataType
     */
    private String generateCustomTypeDefine(String type, String typeName, boolean isSignature) {

        TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null,
                null, createIdentifierToken("public type"),
                createIdentifierToken(typeName),
                createSimpleNameReferenceNode(createIdentifierToken(type)),
                createToken(SEMICOLON_TOKEN));
        generateTypeDefinitionNodeType(typeName, typeDefNode);
        if (!isSignature) {
            return typeName;
        } else {
            return type;
        }
    }

    public void generateTypeDefinitionNodeType(String typeName, TypeDefinitionNode typeDefNode) {
        boolean isExit = false;
        if (!typeDefinitionNodeList.isEmpty()) {
            for (TypeDefinitionNode typeNode: typeDefinitionNodeList) {
                if (typeNode.typeName().toString().trim().equals(typeName)) {
                    isExit = true;
                }
            }
            if (!isExit) {
                typeDefinitionNodeList.add(typeDefNode);
            }
        } else {
            typeDefinitionNodeList.add(typeDefNode);
        }
    }

    /**
     * Get return type of the remote function.
     *
     * @param operation     swagger operation.
     * @return              string with return type.
     * @throws BallerinaOpenApiException - throws exception if creating return type fails.
     */
    public String getReturnType(Operation operation, boolean isSignature) throws BallerinaOpenApiException {
        //TODO: Handle multiple media-type
        String returnType = "http:Response | error";
        if (operation.getResponses() != null) {
            ApiResponses responses = operation.getResponses();
            Collection<ApiResponse> values = responses.values();
            Iterator<ApiResponse> iteratorRes = values.iterator();
            while (iteratorRes.hasNext()) {
                ApiResponse response = iteratorRes.next();
                if (response.getContent() != null) {
                    Content content = response.getContent();
                    Set<Map.Entry<String, MediaType>> mediaTypes = content.entrySet();
                    for (Map.Entry<String, MediaType> media : mediaTypes) {
                        String type = "";
                        if (media.getValue().getSchema() != null) {
                            Schema schema = media.getValue().getSchema();
                            if (schema instanceof ComposedSchema) {
                                ComposedSchema composedSchema = (ComposedSchema) schema;
                                type = generateReturnTypeForComposedSchema(operation, type, composedSchema);
                            } else if (schema instanceof ObjectSchema) {
                                ObjectSchema objectSchema = (ObjectSchema) schema;
                                type = handleInLineRecordInResponse(operation, media, objectSchema.get$ref(),
                                        objectSchema.getProperties(), objectSchema.getRequired());
                            } else if (schema instanceof MapSchema) {
                                MapSchema mapSchema = (MapSchema) schema;
                                type = handleInLineRecordInResponse(operation, media, mapSchema.get$ref(),
                                        mapSchema.getProperties(),
                                        mapSchema.getRequired());
                            } else  if (schema.get$ref() != null) {
                                type = getValidName(extractReferenceType(schema.get$ref()), true);
                                Schema componentSchema = openAPI.getComponents().getSchemas().get(type);
                                if (!isValidSchemaName(type)) {
                                    String operationId = operation.getOperationId();
                                    type = Character.toUpperCase(operationId.charAt(0)) + operationId.substring(1) +
                                            "Response";
                                    List<String> required = componentSchema.getRequired();
                                    Token typeKeyWord = createIdentifierToken("public type");
                                    List<Node> recordFieldList = new ArrayList<>();
                                    Map<String, Schema> properties = componentSchema.getProperties();
                                    String description = "";
                                    if (response.getDescription() != null) {
                                        description = response.getDescription().split("\n")[0];
                                    }
                                    TypeDefinitionNode typeDefinitionNode =
                                            ballerinaSchemaGenerator.getTypeDefinitionNodeForObjectSchema(
                                                    required, typeKeyWord, createIdentifierToken(type), recordFieldList,
                                                    properties, description, openAPI);
                                    generateTypeDefinitionNodeType(type, typeDefinitionNode);
                                }
                            } else if (schema instanceof ArraySchema) {
                                ArraySchema arraySchema = (ArraySchema) schema;
                                // TODO: Nested array when response has
                                type = generateReturnTypeForArraySchema(media, arraySchema, true);
                            } else if (schema.getType() != null) {
                                type = convertOpenAPITypeToBallerina(schema.getType());
                            } else if (media.getKey().trim().equals("application/xml")) {
                                type = generateCustomTypeDefine("xml", "XML", isSignature);
                            } else {
                                type = getBallerinaMeidaType(media.getKey().trim());
                            }
                        } else {
                            type = getBallerinaMeidaType(media.getKey().trim());
                        }

                        StringBuilder builder = new StringBuilder();
                        builder.append(type);
                        builder.append("|");
                        builder.append("error");
                        returnType = builder.toString();
                        // Currently support for first media type
                        break;
                    }
                } else {
                    // Handle response has no content type
                    /**
                     * It will return in functionSignature
                     * <pre> returns error? </>
                     * in functionBody it return nothing, no targetType bindings
                     * <pre> _ = check self.clientEp->post(path, request); </>
                     */
                    returnType = "error?";
                }
                // Currently support for first response.
                break;
            }
        }
        return returnType;
    }

    public String generateReturnTypeForArraySchema(Map.Entry<String, MediaType> media, ArraySchema arraySchema,
                                                    boolean isSignature) throws BallerinaOpenApiException {

        String type;
        if (arraySchema.getItems().get$ref() != null) {
            String name = extractReferenceType(arraySchema.getItems().get$ref());
            type = name + "[]";
            String typeName = name + "Arr";
            TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null, null,
                    createIdentifierToken("public type"),
                    createIdentifierToken(typeName),
                    createSimpleNameReferenceNode(createIdentifierToken(type)),
                    createToken(SEMICOLON_TOKEN));
            // Check already typeDescriptor has same name
            generateTypeDefinitionNodeType(typeName, typeDefNode);
            if (!isSignature) {
                type = typeName;
            }
        } else if (arraySchema.getItems().getType() == null) {
            if (media.getKey().trim().equals("application/xml")) {
                type = generateCustomTypeDefine("xml[]", "XMLArr", isSignature);
            } else if (media.getKey().trim().equals("application/pdf") ||
                    media.getKey().trim().equals("image/png") ||
                    media.getKey().trim().equals("application/octet-stream")) {
                String typeName = getBallerinaMeidaType(media.getKey().trim()) + "Arr";
                type = getBallerinaMeidaType(media.getKey().trim());
                type = generateCustomTypeDefine(type, typeName, isSignature);
            } else {
                String typeName = getBallerinaMeidaType(media.getKey().trim()) + "Arr";
                type = getBallerinaMeidaType(media.getKey().trim()) + "[]";
                type = generateCustomTypeDefine(type, typeName, isSignature);
            }
        } else {
            String typeName = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) +
                    "Arr";
            type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) + "[]";
            type = generateCustomTypeDefine(type, typeName, isSignature);
        }
        return type;
    }

    public String generateReturnTypeForComposedSchema(Operation operation, String type, ComposedSchema composedSchema)
            throws BallerinaOpenApiException {

        if (composedSchema.getOneOf() != null) {
            List<Schema> oneOf = composedSchema.getOneOf();
            type = getOneOfUnionType(oneOf);
            //Get oneOfUnionType name
            String typeName = type.replaceAll("\\|", "");
            TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null, null,
                    createIdentifierToken("public type"),
                    createIdentifierToken(typeName),
                    createSimpleNameReferenceNode(createIdentifierToken(type)),
                    createToken(SEMICOLON_TOKEN));
            generateTypeDefinitionNodeType(typeName, typeDefNode);
        } else if (composedSchema.getAllOf() != null) {
            List<Schema> allOf = composedSchema.getAllOf();
            String recordName = "Compound" + getValidName(operation.getOperationId(), true) +
                    "Response";
            List<String> required = composedSchema.getRequired();
            TypeDefinitionNode allOfTypeDefinitionNode = ballerinaSchemaGenerator
                    .getAllOfTypeDefinitionNode(openAPI, new ArrayList<>(), required,
                            createIdentifierToken(recordName), new ArrayList<>(), allOf);
            generateTypeDefinitionNodeType(recordName, allOfTypeDefinitionNode);
            type = recordName;
        }
        return type;
    }

    //Handle inline record by generating record with name for request.
    public String generateRecordForInlineRequestBody(String operationId, RequestBody requestBody,
                                                       Map<String, Schema> properties2, List<String> required2)
            throws BallerinaOpenApiException {

        String paramType;
        Map<String, Schema> properties = properties2;
        operationId = Character.toUpperCase(operationId.charAt(0)) + operationId.substring(1);
        String typeName = operationId + "Request";
        List<String> required = required2;
        List<Node> fields = new ArrayList<>();
        String description = "";
        if (requestBody.getDescription() != null) {
            description = requestBody.getDescription().split("\n")[0];
        }
        TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNodeForObjectSchema(required,
                createIdentifierToken("public type"), createIdentifierToken(typeName), fields,
                properties, description, openAPI);
        generateTypeDefinitionNodeType(typeName, recordNode);
        paramType = typeName;
        return paramType;
    }


    //Handle inline record by generating record with name for response.
    public String handleInLineRecordInResponse(Operation operation, Map.Entry<String, MediaType> media,
                                                 String ref, Map<String, Schema> properties2,
                                                 List<String> required2) throws BallerinaOpenApiException {

        String type;
        type = getValidName(operation.getOperationId(), true) + "Response";
        if (ref != null) {
            type = extractReferenceType(ref.trim());
        } else if (properties2 != null) {
            Map<String, Schema> properties = properties2;
            if (properties.isEmpty()) {
                type = getBallerinaMeidaType(media.getKey().trim());
            } else {
                List<String> required = required2;
                List<Node> recordFieldList = new ArrayList<>();
                String description = "";
                if (operation.getResponses().entrySet().iterator().next().getValue().getDescription() != null) {
                    description = operation.getResponses().entrySet().iterator().next().getValue().getDescription();
                }
                TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNodeForObjectSchema(required,
                        createIdentifierToken("public type"),
                        createIdentifierToken(type), recordFieldList, properties, description, openAPI);
                generateTypeDefinitionNodeType(type, recordNode);
            }
        } else {
            type = getBallerinaMeidaType(media.getKey().trim());
        }
        return type;
    }


}