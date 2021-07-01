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

package io.ballerina.generators;

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.ListBindingPatternNode;
import io.ballerina.compiler.syntax.tree.LiteralValueToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NilLiteralNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.generators.auth.BallerinaAuthConfigGenerator;
import io.ballerina.openapi.cmd.Filter;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariables;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBinaryExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createDefaultableParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createElseBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createForEachStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIndexedExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createListBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNilLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTemplateExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeTestExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BACKTICK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FOREACH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOGICAL_OR_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.error.ErrorMessages.invalidPathParamType;
import static io.ballerina.generators.GeneratorConstants.DELETE;
import static io.ballerina.generators.GeneratorConstants.EXECUTE;
import static io.ballerina.generators.GeneratorConstants.GET;
import static io.ballerina.generators.GeneratorConstants.HEAD;
import static io.ballerina.generators.GeneratorConstants.HTTP;
import static io.ballerina.generators.GeneratorConstants.OPTIONS;
import static io.ballerina.generators.GeneratorConstants.PATCH;
import static io.ballerina.generators.GeneratorConstants.POST;
import static io.ballerina.generators.GeneratorConstants.PUT;
import static io.ballerina.generators.GeneratorConstants.RESPONSE;
import static io.ballerina.generators.GeneratorConstants.TRACE;
import static io.ballerina.generators.GeneratorUtils.buildUrl;
import static io.ballerina.generators.GeneratorUtils.convertOpenAPITypeToBallerina;
import static io.ballerina.generators.GeneratorUtils.createParamAPIDoc;
import static io.ballerina.generators.GeneratorUtils.escapeIdentifier;
import static io.ballerina.generators.GeneratorUtils.extractReferenceType;
import static io.ballerina.generators.GeneratorUtils.getBallerinaMeidaType;
import static io.ballerina.generators.GeneratorUtils.getBallerinaOpenApiType;
import static io.ballerina.generators.GeneratorUtils.getOneOfUnionType;
import static io.ballerina.generators.GeneratorUtils.getValidName;
import static io.ballerina.generators.GeneratorUtils.isValidSchemaName;

/**
 * This Util class use for generating ballerina client file according to given yaml file.
 */
public class BallerinaClientGenerator {
    private static Server server;
    private static Paths paths;
    private static Filter filters;
    private static List<ImportDeclarationNode> imports = new ArrayList<>();
    private static boolean isQuery;
    private static boolean isHeader;
    private static Info info;
    private static List<TypeDefinitionNode> typeDefinitionNodeList = new ArrayList<>();
    private static OpenAPI openAPI;
    private static BallerinaSchemaGenerator ballerinaSchemaGenerator = new BallerinaSchemaGenerator();

    public static SyntaxTree generateSyntaxTree(Path definitionPath, Filter filter) throws IOException,
            BallerinaOpenApiException {
        imports.clear();
        typeDefinitionNodeList.clear();
        isQuery = false;
        // Summaries OpenAPI details
        openAPI = getBallerinaOpenApiType(definitionPath);
        info = openAPI.getInfo();
        //Filter serverUrl
        List<Server> servers = openAPI.getServers();
        server = servers.get(0);
        paths = setOperationId(openAPI.getPaths());
        if (openAPI.getComponents() != null) {
            // Refactor schema name with valid name
            //Create typeDefinitionNode
            Components components = openAPI.getComponents();
            Map<String, Schema> componentsSchemas = components.getSchemas();
            if (componentsSchemas != null) {
                Map<String, Schema> refacSchema = new HashMap<>();
                for (Map.Entry<String, Schema> schemaEntry : componentsSchemas.entrySet()) {
                    String name = getValidName(schemaEntry.getKey(), true);
                    refacSchema.put(name, schemaEntry.getValue());
                }
                openAPI.getComponents().setSchemas(refacSchema);
            }
        }
        filters = filter;
        // 1. Load client template syntax tree
        SyntaxTree syntaxTree = null;
        // Create imports http
        ImportDeclarationNode importForHttp = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , GeneratorConstants.HTTP);
        imports.add(importForHttp);
        addConfigRecordToTypeDefnitionNodeList(openAPI);
        ClassDefinitionNode classDefinitionNode = getClassDefinitionNode();
        ModulePartNode modulePartNode;
        List<ModuleMemberDeclarationNode> nodes =  new ArrayList<>();
        nodes.addAll(typeDefinitionNodeList);
        nodes.add(classDefinitionNode);
        if (isQuery) {
            ImportDeclarationNode url = GeneratorUtils.getImportDeclarationNode(
                    GeneratorConstants.BALLERINA, "url");
            ImportDeclarationNode string = GeneratorUtils.getImportDeclarationNode(
                    GeneratorConstants.BALLERINA, "lang.'string");
            imports.add(url);
            imports.add(string);
            NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
            FunctionDefinitionNode queryParamFunction = getQueryParamPath();

            nodes.add(queryParamFunction);
            if (isHeader) {
                FunctionDefinitionNode headerMap = generateFunctionForHeaderMap();
                nodes.add(headerMap);
            }
            modulePartNode = createModulePartNode(importsList,
                    createNodeList(nodes),
                    createToken(EOF_TOKEN));
        } else {
            if (isHeader) {
                FunctionDefinitionNode headerMap = generateFunctionForHeaderMap();
                nodes.add(headerMap);
            }
            NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
            modulePartNode = createModulePartNode(importsList, createNodeList(nodes),
                    createToken(EOF_TOKEN));
        }
        TextDocument textDocument = TextDocuments.from("");
        syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

    /**
     * Generate serverUrl for client default value.
     */
    private static String getServerURL(Server server) throws BallerinaOpenApiException {
        String serverURL;
        if (server != null) {
            if (server.getUrl() == null) {
                serverURL = "http://localhost:9090/v1";
            } else if (server.getVariables() != null) {
                ServerVariables variables = server.getVariables();
                URL url;
                String resolvedUrl = buildUrl(server.getUrl(), variables);
                try {
                    url = new URL(resolvedUrl);
                    serverURL = url.toString();
                } catch (MalformedURLException e) {
                    throw new BallerinaOpenApiException("Failed to read endpoint details of the server: " +
                            server.getUrl(), e);
                }
            } else {
                serverURL = server.getUrl();
            }
        } else {
            serverURL = "http://localhost:9090/v1";
        }
        return  serverURL;
    }

    /**
     * Generate Class definition Node.
     */
    private static ClassDefinitionNode getClassDefinitionNode() throws BallerinaOpenApiException {

        // Generate client class
        Token visibilityQualifier = createIdentifierToken(GeneratorConstants.PUBLIC);
        Token clientKeyWord = createIdentifierToken(GeneratorConstants.CLIENT);
        NodeList<Token> classTypeQualifiers = createNodeList(clientKeyWord);

        IdentifierToken classKeyWord = createIdentifierToken(GeneratorConstants.CLASS);
        IdentifierToken className = createIdentifierToken(GeneratorConstants.CLIENT_CLASS);
        Token openBrace = createToken(OPEN_BRACE_TOKEN);
        //Fill the members for class definition node
        List<Node> memberNodeList =  new ArrayList<>();
        //Create class field
        List<ObjectFieldNode> fieldNodeList = getClassField();
        memberNodeList.addAll(fieldNodeList);
        //Create init function definition
        //Common Used
        NodeList<Token> qualifierList = createNodeList(createIdentifierToken(GeneratorConstants.PUBLIC_ISOLATED));
        IdentifierToken functionKeyWord = createIdentifierToken(GeneratorConstants.FUNCTION);
        IdentifierToken functionName = createIdentifierToken("init");
        //Create function signature
        //Add parameters
        List<Node> parameters  = new ArrayList<>();
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        //get config parameters relavant to the auth meachnism used
        parameters.addAll(BallerinaAuthConfigGenerator.getConfigParamForClassInit());
        parameters.add(createToken(COMMA_TOKEN));
        BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken("string"));
        IdentifierToken paramName = createIdentifierToken(GeneratorConstants.SERVICE_URL);
        IdentifierToken equalToken = createIdentifierToken("=");
        BasicLiteralNode expression = createBasicLiteralNode(STRING_LITERAL,
                createIdentifierToken('"' + getServerURL(server) + '"'));

        DefaultableParameterNode serviceUrl = createDefaultableParameterNode(annotationNodes, typeName,
                paramName, equalToken, expression);
        parameters.add(serviceUrl);

        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(parameters);

        //Create return type node for inti function
        IdentifierToken returnsKeyWord = createIdentifierToken(GeneratorConstants.RETURN);
        OptionalTypeDescriptorNode type = createOptionalTypeDescriptorNode(createIdentifierToken("error"),
                createIdentifierToken("?"));
        ReturnTypeDescriptorNode returnNode = createReturnTypeDescriptorNode(returnsKeyWord, annotationNodes, type);

        //Create function signature
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN), returnNode);

        VariableDeclarationNode sslDeclarationNode = BallerinaAuthConfigGenerator.getSecureSocketInitNode();
        //Create function body node
        VariableDeclarationNode clientInitializationNode = BallerinaAuthConfigGenerator.getClientInitializationNode();

        //Assigment for client
        FieldAccessExpressionNode varRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken("self")), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("clientEp")));

        SimpleNameReferenceNode expr = createSimpleNameReferenceNode(createIdentifierToken("httpEp"));
        AssignmentStatementNode httpClientAssignmentStatementNode = createAssignmentStatementNode(varRef,
                createToken(EQUAL_TOKEN), expr, createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode assignmentStatementNodeApiKey = BallerinaAuthConfigGenerator.
                getApiKeyAssignmentNode();

        List<StatementNode> assignmentNodes = new ArrayList<>();
        if (sslDeclarationNode != null) {
            assignmentNodes.add(sslDeclarationNode);
        }
        assignmentNodes.add(clientInitializationNode);
        assignmentNodes.add(httpClientAssignmentStatementNode);
        if (assignmentStatementNodeApiKey != null) {
            assignmentNodes.add(assignmentStatementNodeApiKey);
        }
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        //statementList.addAll(assignmentStatementNodes);

        FunctionBodyNode functionBodyNode = createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN));
        FunctionDefinitionNode initFunctionNode = createFunctionDefinitionNode(null, null,
                qualifierList, functionKeyWord, functionName, createEmptyNodeList(), functionSignatureNode
                , functionBodyNode);

        memberNodeList.add(initFunctionNode);
        // Generate remote function Nodes
        memberNodeList.addAll(createRemoteFunctions(paths, filters));
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        if (info.getExtensions() != null) {
            Map<String, Object> extensions = info.getExtensions();
            if (!extensions.isEmpty()) {
                for (Map.Entry<String, Object> extension: extensions.entrySet()) {
                    if (extension.getKey().trim().equals("x-display")) {
                        metadataNode = getMetadataNodeForDisplayAnnotation(extension);
                    }
                }
            }
        }
        // Generate api doc
        List<Node> documentationLines = new ArrayList<>();
        if (info.getDescription() != null) {
            MarkdownDocumentationLineNode clientDescription =
                    createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                            createNodeList(createLiteralValueToken(null, info.getDescription().split("\n")[0],
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList())));
            documentationLines.add(clientDescription);
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            documentationLines.add(newLine);
        }
        MarkdownParameterDocumentationLineNode httpClientParam = createParamAPIDoc("clientEp",
                "Connector http endpoint");
        documentationLines.add(httpClientParam);
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(createNodeList(documentationLines));
        metadataNode = metadataNode.modify(apiDoc, metadataNode.annotations());
        return createClassDefinitionNode(metadataNode, visibilityQualifier, classTypeQualifiers,
                classKeyWord, className, openBrace, createNodeList(memberNodeList),
                createToken(CLOSE_BRACE_TOKEN));
    }

    /**
     * Generate Client class attributes.
     */
    private static List<ObjectFieldNode> getClassField() {
        List<ObjectFieldNode> fieldNodeList = new ArrayList<>();
        NodeList<Token> qualifierList = createEmptyNodeList();
        QualifiedNameReferenceNode typeName = createQualifiedNameReferenceNode(createIdentifierToken(HTTP),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CLIENT_CLASS));
        IdentifierToken fieldName = createIdentifierToken(GeneratorConstants.CLIENT_EP);
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode httpClientField = createObjectFieldNode(metadataNode, null,
                qualifierList, typeName, fieldName, null, null, createToken(SEMICOLON_TOKEN));
        fieldNodeList.add(httpClientField);
        // add apiKey instance variable when API key security schema is given
        ObjectFieldNode apiKeyFieldNode = BallerinaAuthConfigGenerator.getApiKeyMapClassVariable();
        if (apiKeyFieldNode != null) {
            fieldNodeList.add(apiKeyFieldNode);
        }
        return fieldNodeList;
    }

    /**
     * Generate remote function method name , when operation ID is not available for given operation.
     *
     * @param paths - swagger paths object
     * @return {@link io.swagger.v3.oas.models.Paths }
     */
    private static Paths setOperationId(Paths paths) {
        Set<Map.Entry<String, PathItem>> entries = paths.entrySet();
        for (Map.Entry<String, PathItem> entry: entries) {
            PathItem pathItem = entry.getValue();
            int countMissId = 0;
            for (Operation operation : entry.getValue().readOperations()) {
                if (operation.getOperationId() == null) {
                    //simplify here with 1++
                    countMissId = countMissId + 1;
                } else {
                    String operationId = getValidName(operation.getOperationId(), false);
                    operation.setOperationId(operationId);
                }
            }

            if (pathItem.getGet() != null) {
                Operation getOp = pathItem.getGet();
                if (getOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, GET);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    getOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPut() != null) {
                Operation putOp = pathItem.getPut();
                if (putOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, PUT);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    putOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPost() != null) {
                Operation postOp = pathItem.getPost();
                if (postOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, "post");
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    postOp.setOperationId(operationId);
                }
            }
            if (pathItem.getDelete() != null) {
                Operation deleteOp = pathItem.getDelete();
                if (deleteOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, DELETE);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    deleteOp.setOperationId(operationId);
                }
            }
            if (pathItem.getOptions() != null) {
                Operation optionOp = pathItem.getOptions();
                if (optionOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, OPTIONS);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    optionOp.setOperationId(operationId);
                }
            }
            if (pathItem.getHead() != null) {
                Operation headOp = pathItem.getHead();
                if (headOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, HEAD);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    headOp.setOperationId(operationId);
                }
            }
            if (pathItem.getPatch() != null) {
                Operation patchOp = pathItem.getPatch();
                if (patchOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, PATCH);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    patchOp.setOperationId(operationId);
                }
            }
            if (pathItem.getTrace() != null) {
                Operation traceOp = pathItem.getTrace();
                if (traceOp.getOperationId() == null) {
                    String operationId;
                    String[] split = entry.getKey().trim().split("/");
                    if (countMissId > 1) {
                        operationId = getOperationId(split, TRACE);
                    } else {
                        operationId = getOperationId(split, " ");
                    }
                    traceOp.setOperationId(operationId);
                }
            }
        }
        return paths;
    }

    private static String getOperationId(String[] split, String method) {
        String operationId;
        String regEx = "\\{([^}]*)\\}";
        Matcher matcher = Pattern.compile(regEx).matcher(split[split.length - 1]);
        if (matcher.matches()) {
            operationId = method + split[split.length - 2] + "By" + matcher.group(1);
        } else {
            operationId = method + split[split.length - 1];
        }
        return Character.toLowerCase(operationId.charAt(0)) + operationId.substring(1);
    }

    /**
     * Generate remote functions for OpenAPI operations.
     *
     * @param paths     openAPI Paths
     * @param filter    user given tags and operations
     * @return          FunctionDefinitionNodes list
     * @throws BallerinaOpenApiException - throws when creating remote functions fails
     */
    private static List<FunctionDefinitionNode> createRemoteFunctions(Paths paths, Filter filter)
            throws BallerinaOpenApiException {
        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();
        Set<Map.Entry<String, PathItem>> pathsItems = paths.entrySet();
        for (Map.Entry<String, PathItem> path : pathsItems) {
            if (!path.getValue().readOperationsMap().isEmpty()) {
                Map<PathItem.HttpMethod, Operation> operationMap = path.getValue().readOperationsMap();
                for (Iterator<Map.Entry<PathItem.HttpMethod, Operation>> iter = operationMap.entrySet().iterator();
                     iter.hasNext(); ) {
                    Map.Entry<PathItem.HttpMethod, Operation> operation = iter.next();
                    //Add filter availability
                    //1.Tag filter
                    //2.Operation filter
                    //3.Both tag and operation filter
                    List<String> filterTags = filter.getTags();
                    List<String> operationTags = operation.getValue().getTags();
                    List<String> filterOperations = filter.getOperations();
                    // Handle the display annotations
                    MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
                    Map<String, Object> extensions = operation.getValue().getExtensions();
                    if (extensions != null) {
                        for (Map.Entry<String, Object> extension: extensions.entrySet()) {
                            if (extension.getKey().trim().equals("x-display")) {
                                metadataNode = getMetadataNodeForDisplayAnnotation(extension);
                            }
                        }
                    }

                    if (!filterTags.isEmpty() || !filterOperations.isEmpty()) {
                        if (operationTags != null || ((!filterOperations.isEmpty())
                                && (operation.getValue().getOperationId() != null))) {
                            if (GeneratorUtils.hasTags(operationTags, filterTags) ||
                                    ((operation.getValue().getOperationId() != null) &&
                                            filterOperations.contains(operation.getValue().getOperationId().trim()))) {
                                // function call for generate function definition node.
                                FunctionDefinitionNode functionDefinitionNode =
                                        getFunctionDefinitionNode(metadataNode, path.getKey()
                                                , operation);
                                functionDefinitionNodeList.add(functionDefinitionNode);
                            }
                        }
                    } else {
                        FunctionDefinitionNode functionDefinitionNode = getFunctionDefinitionNode(metadataNode,
                                path.getKey(),
                                operation);
                        functionDefinitionNodeList.add(functionDefinitionNode);
                    }
                }
            }
        }
        return functionDefinitionNodeList;
    }

    /**
     * Generate function definition node.
     */
    private static FunctionDefinitionNode getFunctionDefinitionNode(MetadataNode metadataNode, String path,
                                                                    Map.Entry<PathItem.HttpMethod,
            Operation> operation) throws BallerinaOpenApiException {
        // Create api doc for function
        List<Node> remoteFunctionDocs = new ArrayList<>();
        if (operation.getValue().getSummary() != null) {
            MarkdownDocumentationLineNode clientDescription =
                    createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                            createNodeList(createLiteralValueToken(null, operation.getValue().getSummary(),
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList())));
            remoteFunctionDocs.add(clientDescription);
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            remoteFunctionDocs.add(newLine);
        } else {
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            remoteFunctionDocs.add(newLine);
        }

        //Create qualifier list
        NodeList<Token> qualifierList = createNodeList(createIdentifierToken("remote isolated"));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(operation.getValue().getOperationId());
        NodeList<Node> relativeResourcePath = createEmptyNodeList();

        FunctionSignatureNode functionSignatureNode = getFunctionSignatureNode(operation.getValue(),
                remoteFunctionDocs);
        // Create metadataNode add documentation string
        metadataNode = metadataNode.modify(createMarkdownDocumentationNode(createNodeList(remoteFunctionDocs)),
                metadataNode.annotations());

        // Create Function Body
        FunctionBodyNode functionBodyNode = getFunctionBodyNode(path, operation);

        return createFunctionDefinitionNode(null,
                metadataNode, qualifierList, functionKeyWord, functionName, relativeResourcePath,
                functionSignatureNode, functionBodyNode);
    }

    /**
     * This function for generate function signatures.
     *
     * @param operation openapi operation
     * @return {@link io.ballerina.compiler.syntax.tree.FunctionSignatureNode}
     * @throws BallerinaOpenApiException - throws exception when node creation fails.
     */
    public static FunctionSignatureNode getFunctionSignatureNode(Operation operation, List<Node> remoteFunctionDoc)
            throws BallerinaOpenApiException {
        // Create Parameters - function with parameters
        // Function RequestBody
        List<Node> parameterList =  new ArrayList<>();
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        Token comma =  createToken(COMMA_TOKEN);
        setFunctionParameters(operation, parameterList, comma, remoteFunctionDoc);
        if (parameterList.size() >= 2) {
            parameterList.remove(parameterList.size() - 1);
        }
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);
        //Create Return type - function with response
        Token returnsKeyWord = createToken(RETURNS_KEYWORD);
        //Type Always Union
        String returnType = getReturnType(operation);
        ApiResponses responses = operation.getResponses();
        Collection<ApiResponse> values = responses.values();
        Iterator<ApiResponse> iteratorRes = values.iterator();
        ApiResponse next = iteratorRes.next();
        if (next.getDescription() != null) {
            MarkdownParameterDocumentationLineNode returnDoc = createParamAPIDoc("return",
                    next.getDescription().split("\n")[0]);
            remoteFunctionDoc.add(returnDoc);
        }

        // Default
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(returnsKeyWord,
                annotationNodes, createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(returnType)));

        return createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN), parameters, createToken(CLOSE_PAREN_TOKEN),
                returnTypeDescriptorNode);
    }

    /*
     * Generate function parameters.
     */
    private static void setFunctionParameters(Operation operation, List<Node> parameterList, Token comma,
                                              List<Node> remoteFunctionDoc) throws BallerinaOpenApiException {

        List<Parameter> parameters = operation.getParameters();
        List<MarkdownParameterDocumentationLineNode> defaultParam = new ArrayList<>();
        List<Node> defaultable = new ArrayList<>();
        if (parameters != null) {
            for (Parameter parameter: parameters) {
                String in = parameter.getIn();
                switch (in) {
                    case "path":
                        Node param = getPathParameters(parameter);
                        if (param instanceof RequiredParameterNode) {
                            parameterList.add(param);
                            parameterList.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(escapeIdentifier(getValidName(parameter.getName(),
                                                false)), parameter.getDescription().split("\n")[0]);
                                remoteFunctionDoc.add(paramAPIDoc);
                            }
                        } else {
                            defaultable.add(param);
                            defaultable.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(escapeIdentifier(getValidName(parameter.getName(),
                                                false)), parameter.getDescription().split("\n")[0]);
                                defaultParam.add(paramAPIDoc);
                            }
                        }
                        break;
                    case "query":
                        Node paramq = getQueryParameters(parameter);
                        if (paramq instanceof RequiredParameterNode) {
                            parameterList.add(paramq);
                            parameterList.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(escapeIdentifier(getValidName(parameter.getName(),
                                                false)), parameter.getDescription().split("\n")[0]);
                                remoteFunctionDoc.add(paramAPIDoc);
                            }
                        } else {
                            defaultable.add(paramq);
                            defaultable.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(escapeIdentifier(getValidName(parameter.getName(),
                                                false)), parameter.getDescription().split("\n")[0]);
                                defaultParam.add(paramAPIDoc);
                            }
                        }
                        break;
                    case "header":
                        Node paramh = getHeaderParameter(parameter);
                        if (paramh instanceof RequiredParameterNode) {
                            parameterList.add(paramh);
                            parameterList.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(getValidName(parameter.getName(), false),
                                                parameter.getDescription().split("\n")[0]);
                                remoteFunctionDoc.add(paramAPIDoc);
                            }
                        } else {
                            defaultable.add(paramh);
                            defaultable.add(comma);
                            if (parameter.getDescription() != null) {
                                MarkdownParameterDocumentationLineNode paramAPIDoc =
                                        createParamAPIDoc(getValidName(parameter.getName(), false),
                                                parameter.getDescription().split("\n")[0]);
                                defaultParam.add(paramAPIDoc);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        // Handle RequestBody
        if (operation.getRequestBody() != null) {
            RequestBody requestBody = operation.getRequestBody();
            if (requestBody.getContent() != null) {
                List<Node> requestBodyparam = setRequestBodyParameters(operation.getOperationId(), requestBody,
                        remoteFunctionDoc);
                parameterList.addAll(requestBodyparam);
                parameterList.add(comma);
            } else if (requestBody.get$ref() != null) {
                String requestBodyName = extractReferenceType(requestBody.get$ref());
                RequestBody requestBodySchema = openAPI.getComponents().getRequestBodies().get(requestBodyName.trim());
                List<Node> requestBodyparam = setRequestBodyParameters(operation.getOperationId(), requestBodySchema,
                        remoteFunctionDoc);
                parameterList.addAll(requestBodyparam);
                parameterList.add(comma);
            }
        }
        //Filter defaultable parameters
        if (!defaultable.isEmpty()) {
            parameterList.addAll(defaultable);
            remoteFunctionDoc.addAll(defaultParam);
        }
    }

    /*
     * Create query parameters.
     */
    public static Node getQueryParameters(Parameter parameter) throws BallerinaOpenApiException {
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        TypeDescriptorNode typeName;
        if (parameter.getExtensions() != null) {
             annotationNodes = extractDisplayAnnotation(parameter.getExtensions());
        }

        Schema parameterSchema = parameter.getSchema();

        String paramType = convertOpenAPITypeToBallerina(parameterSchema.getType().trim());
        if (parameterSchema.getType().equals("number")) {
            if (parameterSchema.getFormat() != null) {
                paramType = convertOpenAPITypeToBallerina(parameterSchema.getFormat().trim());
            }
        }

        if (parameterSchema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) parameterSchema;
            if (arraySchema.getItems().getType() != null) {
                String itemType = arraySchema.getItems().getType();
                if (itemType.equals("string") || itemType.equals("integer") || itemType.equals("boolean")
                        || itemType.equals("float") || itemType.equals("decimal")) {
                    paramType = convertOpenAPITypeToBallerina(itemType) + "[]";
                }
            } else if (arraySchema.getItems().get$ref() != null) {
                paramType = extractReferenceType(arraySchema.getItems().get$ref().trim()) + "[]";
            }
        }
        if (parameter.getRequired()) {
             typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
            IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getName().trim(), false));
            return createRequiredParameterNode(annotationNodes, typeName, paramName);
        } else {
            // TODO: for optional change to defaultable with there values
            typeName = createOptionalTypeDescriptorNode(createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(paramType)), createToken(QUESTION_MARK_TOKEN));
            IdentifierToken paramName =
                    createIdentifierToken(escapeIdentifier(getValidName(parameter.getName().trim(), false)));

            if (parameterSchema.getDefault() != null) {
                LiteralValueToken literalValueToken;
                if (parameterSchema.getType().equals("string")) {
                    literalValueToken =
                            createLiteralValueToken(null, '"' + parameterSchema.getDefault().toString() + '"',
                                    createEmptyMinutiaeList(),
                                    createEmptyMinutiaeList());

                } else {
                    literalValueToken =
                            createLiteralValueToken(null, parameterSchema.getDefault().toString(),
                                    createEmptyMinutiaeList(),
                                    createEmptyMinutiaeList());

                }
                return createDefaultableParameterNode(annotationNodes, typeName, paramName, createToken(EQUAL_TOKEN),
                        literalValueToken);
            } else {
                NilLiteralNode nilLiteralNode =
                        createNilLiteralNode(createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN));
                return createDefaultableParameterNode(annotationNodes, typeName, paramName, createToken(EQUAL_TOKEN),
                        nilLiteralNode);
            }
        }
    }

    /*
     * Create path parameters.
     */
    public static Node getPathParameters(Parameter parameter) throws BallerinaOpenApiException {
        NodeList<AnnotationNode> annotationNodes = extractDisplayAnnotation(parameter.getExtensions());
        IdentifierToken paramName = createIdentifierToken(escapeIdentifier(parameter.getName().trim()));
        String type = convertOpenAPITypeToBallerina(parameter.getSchema().getType().trim());
        if (type.equals("anydata") || type.equals("[]") || type.equals("record")) {
            throw new BallerinaOpenApiException(invalidPathParamType(parameter.getName().trim()));
        }
        BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(type));
        return createRequiredParameterNode(annotationNodes, typeName, paramName);
    }

    /*
     * Create header when it comes under the parameter section in swagger.
     */
    private static Node getHeaderParameter(Parameter parameter) throws BallerinaOpenApiException {

        NodeList<AnnotationNode> annotationNodes = extractDisplayAnnotation(parameter.getExtensions());
        if (parameter.getRequired()) {
            String type = convertOpenAPITypeToBallerina(parameter.getSchema().getType().trim());
            Schema schema = parameter.getSchema();
            if (schema instanceof ArraySchema) {
                ArraySchema arraySchema = (ArraySchema) schema;
                if (arraySchema.getItems().get$ref() != null) {
                    type = extractReferenceType(arraySchema.getItems().get$ref()) + "[]";
                } else {
                    type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType().trim()) + "[]";
                }
            }
            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(type));
            IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getName().trim(), false));
            return createRequiredParameterNode(annotationNodes, typeName, paramName);
        } else {
            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(convertOpenAPITypeToBallerina(
                            parameter.getSchema().getType().trim()) + "?"));
            IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getName().trim(), false));
            NilLiteralNode nilLiteralNode =
                    createNilLiteralNode(createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN));
            return createDefaultableParameterNode(annotationNodes, typeName, paramName, createToken(EQUAL_TOKEN),
                    nilLiteralNode);
        }
    }

    /*
     * Create request body parameter.
     */
    private static List<Node> setRequestBodyParameters(String operationId, RequestBody requestBody,
                                                       List<Node> requestBodyDoc)
            throws BallerinaOpenApiException {
        List<Node> parameterList = new ArrayList<>();
        Content content = requestBody.getContent();
        Iterator<Map.Entry<String, MediaType>> iterator = content.entrySet().iterator();
        while (iterator.hasNext()) {
            // This implementation currently for first content type
            Map.Entry<String, MediaType> next = iterator.next();
            Schema schema = next.getValue().getSchema();
            String paramType = "";
            //Take payload type
            if (schema.get$ref() != null) {
                // getValidName is used to get the formmatted schema name
                paramType = getValidName(extractReferenceType(schema.get$ref().trim()), true);
            } else if (schema.getType() != null && !schema.getType().equals("array") && !schema.getType().equals(
                    "object")) {
                String typeOfPayload = schema.getType().trim();
                paramType = convertOpenAPITypeToBallerina(typeOfPayload);
            } else if (schema instanceof ArraySchema) {
                //ToDo: handle nested array
                ArraySchema arraySchema = (ArraySchema) schema;
                if (arraySchema.getItems().getType() != null) {
                    paramType = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) + "[]";
                } else if (arraySchema.getItems().get$ref() != null) {
                    paramType = extractReferenceType(arraySchema.getItems().get$ref()) + "[]";
                } else {
                    paramType = getBallerinaMeidaType(next.getKey().trim()) + "[]";
                }
            } else if (schema instanceof ComposedSchema) {
                // The requestBody only can have oneOf and anyOf data types
                ComposedSchema composedSchema = (ComposedSchema) schema;
                if (composedSchema.getOneOf() != null) {
                    paramType = getOneOfUnionType(composedSchema.getOneOf());
                } else if (composedSchema.getAnyOf() != null) {
                    paramType = getOneOfUnionType(composedSchema.getAnyOf());
                } else if (composedSchema.getAllOf() != null) {
                    paramType = "Compound" +  getValidName(operationId, true) + "Request";
                    List<Schema> allOf = composedSchema.getAllOf();
                    List<String> required = composedSchema.getRequired();
                    TypeDefinitionNode allOfTypeDefinitionNode = ballerinaSchemaGenerator
                            .getAllOfTypeDefinitionNode(openAPI, new ArrayList<>(), required,
                                    createIdentifierToken(paramType), new ArrayList<>(), allOf);
                    generateTypeDefinitionNodeType(paramType, allOfTypeDefinitionNode);
                }
            } else if (schema instanceof ObjectSchema) {
                ObjectSchema objectSchema = (ObjectSchema) schema;
                if (objectSchema.getProperties() != null) {
                    // Generate properties
                    // TODO replace this name generation after merge old PR
                    paramType = generateRecordForInlineRequestBody(operationId, requestBody,
                            objectSchema.getProperties(), objectSchema.getRequired());
                }
            } else if (schema.getProperties() != null) {
                 paramType = generateRecordForInlineRequestBody(operationId, requestBody, schema.getProperties(),
                                schema.getRequired());
            } else {
                paramType = getBallerinaMeidaType(next.getKey());
            }
            if (!paramType.isBlank()) {
                NodeList<AnnotationNode> annotationNodes = extractDisplayAnnotation(requestBody.getExtensions());
                SimpleNameReferenceNode typeName = createSimpleNameReferenceNode(createIdentifierToken(paramType));
                IdentifierToken paramName = createIdentifierToken("payload");
                RequiredParameterNode payload = createRequiredParameterNode(annotationNodes, typeName, paramName);
                if (requestBody.getDescription() != null) {
                    MarkdownParameterDocumentationLineNode paramAPIDoc =
                                createParamAPIDoc(escapeIdentifier("payload"),
                                        requestBody.getDescription().split("\n")[0]);
                        requestBodyDoc.add(paramAPIDoc);
                }
                parameterList.add(payload);
            }
            break;
        }
        return parameterList;
    }

    private static String generateRecordForInlineRequestBody(String operationId, RequestBody requestBody,
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

    /**
     * Extract extension for find the display annotation.
     *
     * @param extensions openapi extension.
     * @return Annotation node list.
     * */
    public static NodeList<AnnotationNode> extractDisplayAnnotation(Map<String, Object> extensions) {
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        if (extensions != null) {
            for (Map.Entry<String, Object> extension: extensions.entrySet()) {
                if (extension.getKey().trim().equals("x-display")) {
                    AnnotationNode annotationNode = getAnnotationNode(extension);
                    annotationNodes = createNodeList(annotationNode);
                }
            }
        }
        return annotationNodes;
    }

    /**
     * Get return type of the remote function.
     *
     * @param operation     swagger operation.
     * @return              string with return type.
     * @throws BallerinaOpenApiException - throws exception if creating return type fails.
     */
    public static String getReturnType(Operation operation) throws BallerinaOpenApiException {
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
                                    return type + "|error";
                                } else if (composedSchema.getAllOf() != null) {
                                    List<Schema> allOf = composedSchema.getAllOf();
                                    String recordName = "Compound" + getValidName(operation.getOperationId(), true) +
                                            "Response";
                                    List<String> required = composedSchema.getRequired();
                                    TypeDefinitionNode allOfTypeDefinitionNode = ballerinaSchemaGenerator
                                            .getAllOfTypeDefinitionNode(openAPI, new ArrayList<>(), required,
                                                    createIdentifierToken(recordName), new ArrayList<>(), allOf);
                                    generateTypeDefinitionNodeType(recordName, allOfTypeDefinitionNode);
                                    return recordName + "|error";
                                }
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
                                    type = typeName;
                                } else if (arraySchema.getItems().getType() == null) {
                                    if (media.getKey().trim().equals("application/xml")) {
                                        type = generateCustomTypeDefine("xml[]", "XMLArr");
                                    } else if (media.getKey().trim().equals("application/pdf") ||
                                            media.getKey().trim().equals("image/png") ||
                                            media.getKey().trim().equals("application/octet-stream")) {
                                        String typeName = getBallerinaMeidaType(media.getKey().trim()) + "Arr";
                                        type = getBallerinaMeidaType(media.getKey().trim());
                                        type = generateCustomTypeDefine(type, typeName);
                                    } else {
                                        String typeName = getBallerinaMeidaType(media.getKey().trim()) + "Arr";
                                        type = getBallerinaMeidaType(media.getKey().trim()) + "[]";
                                        type = generateCustomTypeDefine(type, typeName);
                                    }
                                } else {
                                    String typeName = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) +
                                            "Arr";
                                    type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) + "[]";
                                    type = generateCustomTypeDefine(type, typeName);
                                }
                            } else if (schema.getType() != null) {
                                type = convertOpenAPITypeToBallerina(schema.getType());
                            } else if (media.getKey().trim().equals("application/xml")) {
                                type = generateCustomTypeDefine("xml", "XML");
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

    private static String handleInLineRecordInResponse(Operation operation, Map.Entry<String, MediaType> media,
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
                    description = operation.getResponses().entrySet().iterator().next().getValue().
                            getDescription().split("\n")[0];
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

    /**
     * Generate Type for datatype that can not bind to the targetType.
     * @param type - data Type.
     * @param typeName - Created datType name.
     * @return return dataType
     */
    private static String generateCustomTypeDefine(String type, String typeName) {

        TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null,
                null, createIdentifierToken("public type"),
                createIdentifierToken(typeName),
                createSimpleNameReferenceNode(createIdentifierToken(type)),
                createToken(SEMICOLON_TOKEN));
        generateTypeDefinitionNodeType(typeName, typeDefNode);
        return typeName;
    }

    private static void generateTypeDefinitionNodeType(String typeName, TypeDefinitionNode typeDefNode) {
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
     * Generate function body node.
     *
     * @param path      - remote function path
     * @param operation - opneapi operation
     * @return - function body node
     * @throws BallerinaOpenApiException - throws exception if generating FunctionBodyNode fails.
     */
    public static FunctionBodyNode getFunctionBodyNode(String path, Map.Entry<PathItem.HttpMethod, Operation> operation)
            throws BallerinaOpenApiException {
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        isHeader = false;
        // Create statements
        List<StatementNode> statementsList =  new ArrayList<>();
        // -- create variable declaration
        //string path - common for every remote functions
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(createSimpleNameReferenceNode(
                        createIdentifierToken("string ")), createCaptureBindingPatternNode(
                                createIdentifierToken("path")));
        Token equalToken = createToken(EQUAL_TOKEN);
        // Create initializer
        Token type = createToken(STRING_KEYWORD);
        Token startBacktick = createToken(BACKTICK_TOKEN);
        //content  should decide with /pet and /pet/{pet}
        path = generatePathWithPathParameter(path);

        //String path generator
        NodeList<Node> content = createNodeList(createLiteralValueToken(null, path, createEmptyMinutiaeList(),
                createEmptyMinutiaeList()));
        Token endBacktick = createToken(BACKTICK_TOKEN);
        TemplateExpressionNode initializer = createTemplateExpressionNode(null, type, startBacktick, content,
                endBacktick);
        Token semicolon = createToken(SEMICOLON_TOKEN);
        VariableDeclarationNode pathInt = createVariableDeclarationNode(annotationNodes, null,
                typedBindingPatternNode, equalToken, initializer, semicolon);

        statementsList.add(pathInt);

        //Handel query parameter map
        if (operation.getValue().getParameters() != null) {
            List<Parameter> parameters = operation.getValue().getParameters();
            List<Parameter> queryParameters = new ArrayList<>();
            List<Parameter> headerParameters = new ArrayList<>();
            for (Parameter parameter: parameters) {
                if (parameter.getIn().trim().equals("query")) {
                    queryParameters.add(parameter);
                } else if (parameter.getIn().trim().equals("header")) {
                    headerParameters.add(parameter);
                }
            }

            List<String> queryApiKeyNameList = BallerinaAuthConfigGenerator.getQueryApiKeyNameList();
            List<String> headerApiKeyNameList = BallerinaAuthConfigGenerator.getHeaderApiKeyNameList();

            if (!queryParameters.isEmpty() || !queryApiKeyNameList.isEmpty()) {
                statementsList.add(getMapForParameters(queryParameters, "map<anydata>",
                        "queryParam", queryApiKeyNameList, false));
                // Add updated path
                ExpressionStatementNode updatedPath = getSimpleExpressionStatementNode("path = path + " +
                        "check getPathForQueryParam(queryParam)");
                statementsList.add(updatedPath);
                isQuery = true;
            }

            if (!headerParameters.isEmpty() || !headerApiKeyNameList.isEmpty()) {
                statementsList.add(getMapForParameters(headerParameters, "map<any>",
                        "headerValues", headerApiKeyNameList, true));
                statementsList.add(getSimpleExpressionStatementNode("map<string|string[]> accHeaders = " +
                        "getMapForHeaders(headerValues)"));
                isHeader = true;
            }
        } else {
            List<String> queryApiKeyNameList = BallerinaAuthConfigGenerator.getQueryApiKeyNameList();
            List<String> headerApiKeyNameList = BallerinaAuthConfigGenerator.getHeaderApiKeyNameList();

            if (!queryApiKeyNameList.isEmpty()) {
                statementsList.add(getMapForParameters(new ArrayList<>(), "map<anydata>",
                        "queryParam", queryApiKeyNameList, false));
                // Add updated path
                ExpressionStatementNode updatedPath = getSimpleExpressionStatementNode("path = path + " +
                        "getPathForQueryParam(queryParam)");
                statementsList.add(updatedPath);
                isQuery = true;
            }
            if (!headerApiKeyNameList.isEmpty()) {
                statementsList.add(getMapForParameters(new ArrayList<>(), "map<string|string[]>",
                        "accHeaders", headerApiKeyNameList, true));
                isHeader = false;
            }

        }

        String method = operation.getKey().name().trim().toLowerCase(Locale.ENGLISH);
        String rType = getReturnType(operation.getValue());
        String returnType;
        if (!rType.equals("error?")) {
            int index = rType.lastIndexOf("|");
            returnType = rType.substring(0, index);
            if (returnType.contains("|")) {
                returnType = returnType.replaceAll("\\|", "");
            }
        } else {
            returnType = rType;
        }
        // Statement Generator for requestBody
        if (operation.getValue().getRequestBody() != null) {
            RequestBody requestBody = operation.getValue().getRequestBody();
            if (requestBody.getContent() != null) {
                Content rbContent = requestBody.getContent();
                Set<Map.Entry<String, MediaType>> entries = rbContent.entrySet();
                Iterator<Map.Entry<String, MediaType>> iterator = entries.iterator();
                //Currently align with first content of the requestBody
                while (iterator.hasNext()) {
                    createRequestBodyStatements(isHeader, statementsList, method, returnType, iterator);
                    break;
                }
            } else if (requestBody.get$ref() != null) {
                RequestBody requestBodySchema =
                        openAPI.getComponents().getRequestBodies().get(extractReferenceType(requestBody.get$ref()));
                Content rbContent = requestBodySchema.getContent();
                Set<Map.Entry<String, MediaType>> entries = rbContent.entrySet();
                Iterator<Map.Entry<String, MediaType>> iterator = entries.iterator();
                //currently align with first content of the requestBody
                while (iterator.hasNext()) {
                    createRequestBodyStatements(isHeader, statementsList, method, returnType, iterator);
                    break;
                }
            }
        } else {
            String clientCallStatement;
            if (!rType.equals("error?")) {
                clientCallStatement = "check self.clientEp-> " + method + "(path, targetType = " + returnType + ")";
            } else {
                clientCallStatement = "check self.clientEp-> " + method + "(path, targetType=http:Response)";
            }
            if (isHeader) {
                if (method.equals(POST) || method.equals(PUT) || method.equals(PATCH) || method.equals(
                        DELETE) || method.equals(EXECUTE)) {
                    ExpressionStatementNode requestStatementNode = getSimpleExpressionStatementNode(
                            "http:Request request = new");
                    statementsList.add(requestStatementNode);
                    ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                            "//TODO: Update the request as needed");
                    statementsList.add(expressionStatementNode);
                    if (!rType.equals("error?")) {
                        clientCallStatement = "check self.clientEp-> " + method + "(path, request, headers = " +
                                "accHeaders, targetType = " + returnType + ")";
                    } else {
                        clientCallStatement = "check self.clientEp-> " + method + "(path, request, headers = " +
                                "accHeaders, targetType=http:Response)";
                    }
                } else {
                    if (!rType.equals("error?")) {
                        clientCallStatement = "check self.clientEp-> " + method + "(path, accHeaders, targetType = "
                                        + returnType + ")";
                    } else {
                        clientCallStatement = "check self.clientEp-> " + method + "(path, accHeaders, " +
                                "targetType=http:Response)";
                    }
                }
            } else if (method.equals(POST) || method.equals(PUT) || method.equals(PATCH) || method.equals(DELETE)
                    || method.equals(EXECUTE)) {
                ExpressionStatementNode requestStatementNode = getSimpleExpressionStatementNode(
                        "http:Request request = new");
                statementsList.add(requestStatementNode);
                ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                        "//TODO: Update the request as needed");
                statementsList.add(expressionStatementNode);
                if (!rType.equals("error?")) {
                    clientCallStatement =
                            "check self.clientEp-> " + method + "(path, request, targetType = " + returnType + ")";
                } else {
                    clientCallStatement = "check self.clientEp-> " + method + "(path, request, targetType " +
                            "=http:Response)";
                }
            }
            //Return Variable
            if (!rType.equals("error?")) {
                VariableDeclarationNode clientCall = getSimpleStatement(returnType, RESPONSE, clientCallStatement);
                statementsList.add(clientCall);
                Token returnKeyWord = createIdentifierToken("return");
                SimpleNameReferenceNode returns = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
                ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returns, semicolon);
                statementsList.add(returnStatementNode);
            } else {
                statementsList.add(getSimpleStatement("", "_", clientCallStatement));
            }
        }
        //Create statements
        NodeList<StatementNode> statements = createNodeList(statementsList);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN), null, statements,
                createToken(CLOSE_BRACE_TOKEN));
    }

    /**
     * This function for creating requestBody statements.
     * -- ex: Request body with json payload.
     * <pre>
     *    http:Request request = new;
     *    json jsonBody = check payload.cloneWithType(json);
     *    request.setPayload(jsonBody);
     *    json response = check self.clientEp->put(path, request, targetType=json);
     * </pre>
     *
     * @param isHeader -boolean value for header availability.
     * @param statementsList - StatementNode list in body node
     * @param method         - Operation method name.
     * @param returnType     - Response type
     * @param iterator       - RequestBody media type
     */
    private static void createRequestBodyStatements(boolean isHeader, List<StatementNode> statementsList,
                                                    String method, String returnType,
                                                    Iterator<Map.Entry<String, MediaType>> iterator) {

        //Create Request statement
        Map.Entry<String, MediaType> next = iterator.next();
        VariableDeclarationNode requestVariable = getSimpleStatement("http:Request",
                "request", "new");
        statementsList.add(requestVariable);
        if (next.getValue().getSchema() != null) {
            if (next.getKey().contains("json")) {
                VariableDeclarationNode jsonVariable = getSimpleStatement("json",
                        "jsonBody", "check payload.cloneWithType(json)");
                statementsList.add(jsonVariable);
                ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                        "request.setPayload(jsonBody)");
                statementsList.add(expressionStatementNode);
            } else if (next.getKey().contains("xml")) {
                ImportDeclarationNode xmlImport = GeneratorUtils.getImportDeclarationNode(
                        GeneratorConstants.BALLERINA, "xmldata");
                imports.add(xmlImport);
                VariableDeclarationNode jsonVariable = getSimpleStatement("json",
                        "jsonBody", "check payload.cloneWithType(json)");
                statementsList.add(jsonVariable);
                VariableDeclarationNode xmlBody = getSimpleStatement("xml?", "xmlBody",
                        "check xmldata:fromJson(jsonBody)");
                statementsList.add(xmlBody);
                ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                        "request.setPayload(xmlBody)");
                statementsList.add(expressionStatementNode);
            } else if (next.getKey().contains("plain")) {
                ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                        "request.setPayload(payload)");
                statementsList.add(expressionStatementNode);
            }
            // TODO:Fill with other mime type
        } else {
            // Add default value comment
            ExpressionStatementNode expressionStatementNode = getSimpleExpressionStatementNode(
                    "TODO: Update the request as needed");
            statementsList.add(expressionStatementNode);
        }
        // POST, PUT, PATCH, DELETE, EXECUTE
        VariableDeclarationNode requestStatement =
                getSimpleStatement(returnType, RESPONSE, "check self.clientEp->"
                        + method + "(path," + " request, targetType=" + returnType + ")");
        if (isHeader) {
            if (method.equals(POST) || method.equals(PUT) || method.equals(PATCH) || method.equals(DELETE)
                    || method.equals(EXECUTE)) {
                if (!returnType.equals("error?")) {
                    requestStatement = getSimpleStatement(returnType, RESPONSE,
                            "check self.clientEp->" + method + "(path, request, headers = accHeaders, " +
                                    "targetType=" + returnType + ")");
                    statementsList.add(requestStatement);
                    Token returnKeyWord = createIdentifierToken("return");
                    SimpleNameReferenceNode returns = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
                    ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returns,
                            createToken(SEMICOLON_TOKEN));
                    statementsList.add(returnStatementNode);
                } else {
                    requestStatement = getSimpleStatement("", "_",
                            "check self.clientEp->" + method + "(path, request, headers = accHeaders, " +
                                    "targetType=http:Response)");
                    statementsList.add(requestStatement);
                }
            }
        } else {
            if (!returnType.equals("error?")) {
                statementsList.add(requestStatement);
                Token returnKeyWord = createIdentifierToken("return");
                SimpleNameReferenceNode returnVariable = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
                ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returnVariable,
                        createToken(SEMICOLON_TOKEN));
                statementsList.add(returnStatementNode);
            } else {
                String clientCallStatement = "check self.clientEp-> " + method + "(path, request, targetType"
                        + "=http:Response)";
                statementsList.add(getSimpleStatement("", "_", clientCallStatement));
            }
        }
    }

    /**
     * This method is to used for generating path when it has path parameters.
     *
     * @param path - yaml contract path
     * @return string of path
     */
    public static String generatePathWithPathParameter(String path) {
        if (path.contains("{")) {
            String refinedPath = path;
        Pattern p = Pattern.compile("\\{[^\\}]*\\}");
            Matcher m = p.matcher(path);
            while (m.find()) {
                String d = path.substring(m.start() + 1, m.end() - 1);
                String replaceVariable = escapeIdentifier(d);
                refinedPath = refinedPath.replace(d, replaceVariable);
            }
            path = refinedPath.replaceAll("[{]", "\\${");
        }
        return path;
    }

    /*
     * Generate variableDeclarationNode.
     */
    private static VariableDeclarationNode getSimpleStatement(String responseType, String variable,
                                                              String initializer) {
        SimpleNameReferenceNode resTypeBind = createSimpleNameReferenceNode(createIdentifierToken(responseType));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(createIdentifierToken(variable));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(resTypeBind, bindingPattern);
        SimpleNameReferenceNode init = createSimpleNameReferenceNode(createIdentifierToken(initializer));

        return createVariableDeclarationNode(createEmptyNodeList(), null, typedBindingPatternNode,
                createToken(EQUAL_TOKEN), init, createToken(SEMICOLON_TOKEN));
    }

    private static VariableDeclarationNode getMapForParameters(List<Parameter> parameters, String mapDataType,
                                                               String mapName, List<String> apiKeyNames,
                                                               boolean isHeader) {
        List<Node> filedOfMap = new ArrayList();
        BuiltinSimpleNameReferenceNode mapType = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(mapDataType));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken(mapName));
        TypedBindingPatternNode bindingPatternNode = createTypedBindingPatternNode(mapType, bindingPattern);

        for (Parameter parameter: parameters) {
            // Initializer
            IdentifierToken fieldName = createIdentifierToken('"' + (parameter.getName().trim()) + '"');
            Token colon = createToken(COLON_TOKEN);
            SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(
                    createIdentifierToken(escapeIdentifier(getValidName(parameter.getName().trim(), false))));
            SpecificFieldNode specificFieldNode = createSpecificFieldNode(null,
                    fieldName, colon, valueExpr);
            filedOfMap.add(specificFieldNode);
            filedOfMap.add(createToken(COMMA_TOKEN));
        }

        if (!apiKeyNames.isEmpty()) {
            for (String apiKey : apiKeyNames) {
                IdentifierToken fieldName = createIdentifierToken(escapeIdentifier(apiKey.trim()));
                Token colon = createToken(COLON_TOKEN);
                FieldAccessExpressionNode fieldExpr = createFieldAccessExpressionNode(
                        createSimpleNameReferenceNode(createIdentifierToken("self")), createToken(DOT_TOKEN),
                        createSimpleNameReferenceNode(createIdentifierToken("apiKeys")));
                SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(
                        createIdentifierToken("\"" + apiKey + "\""));
                SpecificFieldNode specificFieldNode;
                if (isHeader) {
                    SeparatedNodeList<FunctionArgumentNode> apiKeyNameArg = createSeparatedNodeList(valueExpr);
                    MethodCallExpressionNode apiKeyExpr = createMethodCallExpressionNode(fieldExpr,
                            createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("get")),
                            createToken(OPEN_PAREN_TOKEN), apiKeyNameArg, createToken(CLOSE_PAREN_TOKEN));
                    specificFieldNode = createSpecificFieldNode(null, fieldName, colon, apiKeyExpr);
                } else {
                    SeparatedNodeList<ExpressionNode> expressions = createSeparatedNodeList(valueExpr);
                    IndexedExpressionNode apiKeyExpr = createIndexedExpressionNode(fieldExpr,
                            createToken(OPEN_BRACKET_TOKEN), expressions, createToken(CLOSE_BRACKET_TOKEN));
                    specificFieldNode = createSpecificFieldNode(null, fieldName, colon, apiKeyExpr);
                }
                filedOfMap.add(specificFieldNode);
                filedOfMap.add(createToken(COMMA_TOKEN));
            }
        }

        filedOfMap.remove(filedOfMap.size() - 1);
        MappingConstructorExpressionNode initialize = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(filedOfMap),
                createToken(CLOSE_BRACE_TOKEN));
        return createVariableDeclarationNode(createEmptyNodeList(),
                null, bindingPatternNode, createToken(EQUAL_TOKEN), initialize,
                createToken(SEMICOLON_TOKEN));
    }

/*
 * Generate expressionStatementNode.
 */
    private static ExpressionStatementNode getSimpleExpressionStatementNode(String expression) {
        SimpleNameReferenceNode expressionNode = createSimpleNameReferenceNode(
                createIdentifierToken(expression));
        return createExpressionStatementNode(null, expressionNode, createToken(SEMICOLON_TOKEN));
    }

    // Create queryPath param function
    private static FunctionDefinitionNode getQueryParamPath() {
        //Create API doc
        List<Node> docs = new ArrayList<>();
        MarkdownDocumentationLineNode functionDescription =
                createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                        createNodeList(createLiteralValueToken(null,
                                "Generate query path with query parameter.",
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())));
        docs.add(functionDescription);
        MarkdownDocumentationLineNode hashNewLine = createMarkdownDocumentationLineNode(null,
                createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
        docs.add(hashNewLine);
        // Create client init description
        MarkdownParameterDocumentationLineNode queryParam = createParamAPIDoc("queryParam",
                "Query parameter map");
        docs.add(queryParam);
        MarkdownParameterDocumentationLineNode returnDoc = createParamAPIDoc("return",
                "Returns generated Path or error at failure of client initialization");
        docs.add(returnDoc);

        MarkdownDocumentationNode functionDoc = createMarkdownDocumentationNode(createNodeList(docs));
        MetadataNode metadataNode = createMetadataNode(functionDoc, createEmptyNodeList());
        Token functionKeyWord = createIdentifierToken("isolated function");
        IdentifierToken functionName = createIdentifierToken(" getPathForQueryParam");
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN),
                        createSeparatedNodeList(createRequiredParameterNode(createEmptyNodeList(),
                                createIdentifierToken("map<anydata> "),
                                createIdentifierToken(" queryParam"))),
                        createToken(CLOSE_PAREN_TOKEN),
                        createReturnTypeDescriptorNode(createIdentifierToken(" returns "),
                                createEmptyNodeList(), createBuiltinSimpleNameReferenceNode(
                                        null, createIdentifierToken("string|error"))));

        // FunctionBody
        List<StatementNode> statementNodes = new ArrayList<>();
        VariableDeclarationNode variable = getSimpleStatement("string[]", "param", "[]");
        statementNodes.add(variable);
        ExpressionStatementNode assign = getSimpleExpressionStatementNode("param[param.length()] = \"?\"");
        statementNodes.add(assign);

        // Create for each loop
        Token forEachKeyWord = createToken(FOREACH_KEYWORD);

        BuiltinSimpleNameReferenceNode type = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(" var"));
        ListBindingPatternNode bindingPattern =
                createListBindingPatternNode(createToken(OPEN_BRACKET_TOKEN),
                createSeparatedNodeList(createCaptureBindingPatternNode(
                        createIdentifierToken("key")), createToken(COMMA_TOKEN),
                        createCaptureBindingPatternNode(createIdentifierToken("value"))),
                        createToken(CLOSE_BRACKET_TOKEN));

        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(type,
                bindingPattern);

        Token inKeyWord = createToken(IN_KEYWORD);
        SimpleNameReferenceNode expr = createSimpleNameReferenceNode(createIdentifierToken(" queryParam"));
        Token dotToken = createToken(DOT_TOKEN);
        SimpleNameReferenceNode methodName = createSimpleNameReferenceNode(createIdentifierToken("entries"));
        MethodCallExpressionNode actionOrExpr = createMethodCallExpressionNode(expr, dotToken, methodName
                , createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN));
        // block statement
        // if-else statements
        Token ifKeyWord = createToken(IF_KEYWORD);
        // Create 'value is ()' statement
        SimpleNameReferenceNode expression = createSimpleNameReferenceNode(createIdentifierToken(" value "));
        Token isKeyWord = createToken(IS_KEYWORD);
        BuiltinSimpleNameReferenceNode typeCondition = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(" ()"));
        TypeTestExpressionNode mainCondition = createTypeTestExpressionNode(expression, isKeyWord,
                typeCondition);
        // If body
        ExpressionStatementNode assignStatement = getSimpleExpressionStatementNode("_ = queryParam.remove(key)");
        BlockStatementNode ifBlockStatementMain = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                        createNodeList(assignStatement), createToken(CLOSE_BRACE_TOKEN));
        //else body
        Token elseKeyWord = createToken(ELSE_KEYWORD);
        //body statements
        FunctionCallExpressionNode condition = createFunctionCallExpressionNode(createSimpleNameReferenceNode(
                        createIdentifierToken(" string:startsWith")), createToken(OPEN_PAREN_TOKEN),
                        createSeparatedNodeList(
                                createSimpleNameReferenceNode(createIdentifierToken(" key")),
                                createToken(COMMA_TOKEN), createSimpleNameReferenceNode(
                                        createIdentifierToken("\"'\""))), createToken(CLOSE_PAREN_TOKEN));
        List<StatementNode> statements = new ArrayList<>();
        // if body-02
        ExpressionStatementNode ifBody02Statement = getSimpleExpressionStatementNode(" param[param.length()] = " +
                "string:substring(key, 1, key.length())");

        NodeList<StatementNode> statementNodesForIf02 = createNodeList(ifBody02Statement);
        BlockStatementNode ifBlock02 = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                        statementNodesForIf02, createToken(CLOSE_BRACE_TOKEN));

        // else block-02
        // else body 02

        ExpressionStatementNode elseBody02Statement = getSimpleExpressionStatementNode("param[param.length()] = key");
        NodeList<StatementNode> statementNodesForElse02 = createNodeList(elseBody02Statement);
        BlockStatementNode elseBlockNode02 = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                        statementNodesForElse02, createToken(CLOSE_BRACE_TOKEN));

        ElseBlockNode elseBlock02 = createElseBlockNode(createToken(ELSE_KEYWORD), elseBlockNode02);
        IfElseStatementNode ifElseStatementNode02 = createIfElseStatementNode(ifKeyWord, condition, ifBlock02,
                elseBlock02);
        statements.add(ifElseStatementNode02);

        ExpressionStatementNode assignment = getSimpleExpressionStatementNode("param[param.length()] = \"=\"");
        statements.add(assignment);

        //If block 03
        SimpleNameReferenceNode exprIf03 = createSimpleNameReferenceNode(createIdentifierToken(" value "));
        BuiltinSimpleNameReferenceNode typeCondition03 = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(" string"));
        TypeTestExpressionNode condition03 = createTypeTestExpressionNode(exprIf03, isKeyWord, typeCondition03);

        ExpressionStatementNode variableIf03 = getSimpleExpressionStatementNode("string updateV =  check " +
                "url:encode(value, \"UTF-8\")");
        ExpressionStatementNode assignIf03 = getSimpleExpressionStatementNode("param[param.length()] = updateV");

        BlockStatementNode ifBody03 = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(variableIf03, assignIf03), createToken(CLOSE_BRACE_TOKEN));
        BlockStatementNode elseBodyBlock03 = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), createNodeList(
                        getSimpleExpressionStatementNode("param[param.length()] = value.toString()")),
                createToken(CLOSE_BRACE_TOKEN));
        ElseBlockNode elseBody03 = createElseBlockNode(elseKeyWord, elseBodyBlock03);
        IfElseStatementNode ifElse03 = createIfElseStatementNode(ifKeyWord, condition03, ifBody03, elseBody03);

        statements.add(ifElse03);

        ExpressionStatementNode andStatement = getSimpleExpressionStatementNode("param[param.length()] = \"&\"");
        statements.add(andStatement);

        NodeList<StatementNode> elseBodyStatements = createNodeList(statements);

        BlockStatementNode elseBlock = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), elseBodyStatements,
                createToken(CLOSE_BRACE_TOKEN));

        ElseBlockNode elseBlockMain = createElseBlockNode(elseKeyWord, elseBlock);

        IfElseStatementNode mainIfElse = createIfElseStatementNode(ifKeyWord, mainCondition,
                ifBlockStatementMain, elseBlockMain);

        //For each block statement
        BlockStatementNode forEachBlockStatement = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                        createNodeList(mainIfElse), createToken(CLOSE_BRACE_TOKEN));

        ForEachStatementNode forEachStatementNode = createForEachStatementNode(forEachKeyWord,
                typedBindingPatternNode, inKeyWord, actionOrExpr, forEachBlockStatement, null);

        statementNodes.add(forEachStatementNode);

        //Remove last `&` statement
        ExpressionStatementNode assignLine02 = getSimpleExpressionStatementNode("_ = param.remove(param.length()-1)");
        statementNodes.add(assignLine02);

        //IfElseStatement
        SimpleNameReferenceNode lhs = createSimpleNameReferenceNode(createIdentifierToken(" param.length()"));
        Token equalToken = createIdentifierToken("==");
        BuiltinSimpleNameReferenceNode rhs = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(" 1"));
        TypeTestExpressionNode conditionForIfElse = createTypeTestExpressionNode(lhs, equalToken, rhs);
        //if body block

        ExpressionStatementNode newAssign = getSimpleExpressionStatementNode("_ = param.remove(0)");
        BlockStatementNode ifBlock = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                        createNodeList(newAssign), createToken(CLOSE_BRACE_TOKEN));
        IfElseStatementNode ifElseStatementNode = createIfElseStatementNode(ifKeyWord, conditionForIfElse, ifBlock,
                null);
        statementNodes.add(ifElseStatementNode);

        statementNodes.add(getSimpleExpressionStatementNode("string restOfPath = string:'join(\"\", ...param)"));
        statementNodes.add(getSimpleExpressionStatementNode("return restOfPath"));
        FunctionBodyNode functionBodyNode = createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, createNodeList(statementNodes), createToken(CLOSE_BRACE_TOKEN));

        return createFunctionDefinitionNode(FUNCTION_DEFINITION, metadataNode,
                createEmptyNodeList(), functionKeyWord, functionName, createEmptyNodeList(),
                functionSignatureNode, functionBodyNode);
    }

    /*
     * Generate metaDataNode with display annotation.
     */
    private static MetadataNode getMetadataNodeForDisplayAnnotation(Map.Entry<String, Object> extension) {

        MetadataNode metadataNode;
        AnnotationNode annotationNode = getAnnotationNode(extension);
        metadataNode = createMetadataNode(null, createNodeList(annotationNode));
        return metadataNode;
    }

    private static AnnotationNode getAnnotationNode(Map.Entry<String, Object> extension) {

        LinkedHashMap<String, String> extFields = (LinkedHashMap<String, String>) extension.getValue();
        List<Node> annotFields = new ArrayList<>();
        if (!extFields.isEmpty()) {
            for (Map.Entry<String, String> field: extFields.entrySet()) {

                BasicLiteralNode valueExpr = createBasicLiteralNode(STRING_LITERAL,
                        createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                '"' + field.getValue().trim() + '"',
                                createEmptyMinutiaeList(),
                                createEmptyMinutiaeList()));
                SpecificFieldNode fields = createSpecificFieldNode(null,
                        createIdentifierToken(field.getKey().trim()),
                        createToken(COLON_TOKEN), valueExpr);
                annotFields.add(fields);
                annotFields.add(createToken(COMMA_TOKEN));
            }
            if (annotFields.size() == 2) {
                annotFields.remove(1);
            }
        }

        MappingConstructorExpressionNode annotValue = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(annotFields),
                createToken(CLOSE_BRACE_TOKEN));

        SimpleNameReferenceNode annotateReference =
                createSimpleNameReferenceNode(createIdentifierToken("display"));

        return createAnnotationNode(createToken(SyntaxKind.AT_TOKEN)
                , annotateReference, annotValue);
    }

    private static void addConfigRecordToTypeDefnitionNodeList(OpenAPI openAPI) {
        TypeDefinitionNode configRecord = BallerinaAuthConfigGenerator.getConfigRecord(openAPI);
        if (configRecord != null) {
            typeDefinitionNodeList.add(configRecord);
        }
    }

    /** Generate function for functionDefinition Node.
     *
     * <pre>
     *     isolated function getMapForHeaders(map<any> headerParam) returns map<string|string[]> {
     *     map<string|string[]> headerMap = {};
     *     foreach var [key, value] in headerParam.entries() {
     *         if value is string {
     *             headerMap[key] = value;
     *         }
     *     }
     *     return headerMap;
     * }
     * </pre>
     *
     * @return functionDefinitionNode
     */
    private static FunctionDefinitionNode generateFunctionForHeaderMap() {
        // API doc for function
        List<Node> docs = new ArrayList<>();
        MarkdownDocumentationLineNode functionDescription =
                createMarkdownDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                        createNodeList(createLiteralValueToken(null,
                                "Generate header map for given header values.",
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())));
        docs.add(functionDescription);
        MarkdownDocumentationLineNode hashNewLine = createMarkdownDocumentationLineNode(null,
                createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
        docs.add(hashNewLine);
        // Create client init description
        MarkdownParameterDocumentationLineNode queryParam = createParamAPIDoc("headerParam",
                "Headers  map");
        docs.add(queryParam);
        MarkdownParameterDocumentationLineNode returnDoc = createParamAPIDoc("return",
                "Returns generated map or error at failure of client initialization");
        docs.add(returnDoc);

        MarkdownDocumentationNode functionDoc = createMarkdownDocumentationNode(createNodeList(docs));
        MetadataNode metadataNode = createMetadataNode(functionDoc, createEmptyNodeList());

        Token functionKeyWord = createIdentifierToken("isolated function");
        IdentifierToken functionName = createIdentifierToken(" getMapForHeaders");
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(createRequiredParameterNode(createEmptyNodeList(),
                        createIdentifierToken("map<any> "),
                        createIdentifierToken(" headerParam"))),
                createToken(CLOSE_PAREN_TOKEN),
                createReturnTypeDescriptorNode(createIdentifierToken(" returns "),
                        createEmptyNodeList(), createBuiltinSimpleNameReferenceNode(
                                null, createIdentifierToken("map<string|string[]>"))));

        List<StatementNode> statementNodes = new ArrayList<>();
        VariableDeclarationNode headerMap = getSimpleStatement("map<string|string[]>",
                "headerMap", "{}");
        statementNodes.add(headerMap);
        // Create foreach loop
        Token forEachKeyWord = createToken(FOREACH_KEYWORD);

        BuiltinSimpleNameReferenceNode type = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(" var"));
        ListBindingPatternNode bindingPattern =
                createListBindingPatternNode(createToken(OPEN_BRACKET_TOKEN),
                        createSeparatedNodeList(createCaptureBindingPatternNode(
                                createIdentifierToken("key")), createToken(COMMA_TOKEN),
                                createCaptureBindingPatternNode(createIdentifierToken("value"))),
                        createToken(CLOSE_BRACKET_TOKEN));

        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(type,
                bindingPattern);

        Token inKeyWord = createToken(IN_KEYWORD);
        SimpleNameReferenceNode expr = createSimpleNameReferenceNode(createIdentifierToken(" headerParam"));
        Token dotToken = createToken(DOT_TOKEN);
        SimpleNameReferenceNode methodName = createSimpleNameReferenceNode(createIdentifierToken("entries"));
        MethodCallExpressionNode actionOrExpr = createMethodCallExpressionNode(expr, dotToken, methodName
                , createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN));
        //Foreach block statement
        List<StatementNode> foreachBlock = new ArrayList<>();
        // if-else statements
        Token ifKeyWord = createToken(IF_KEYWORD);
        // Create 'value is string' statement
        SimpleNameReferenceNode expression = createSimpleNameReferenceNode(createIdentifierToken(" value "));
        Token isKeyWord = createToken(IS_KEYWORD);
        BuiltinSimpleNameReferenceNode lhd = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(" string"));
        TypeTestExpressionNode lhdCondition = createTypeTestExpressionNode(expression, isKeyWord,
                lhd);
        // Create 'value is string[]' statement
        BuiltinSimpleNameReferenceNode rhd = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(" string[]"));
        TypeTestExpressionNode rhdCondition = createTypeTestExpressionNode(expression, isKeyWord,
                rhd);

        BinaryExpressionNode mainCondition = createBinaryExpressionNode(null, lhdCondition,
                createToken(LOGICAL_OR_TOKEN), rhdCondition);
        ExpressionStatementNode assignStatement = getSimpleExpressionStatementNode("headerMap[key] = value");
        BlockStatementNode ifBlockStatementMain = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(assignStatement), createToken(CLOSE_BRACE_TOKEN));
        IfElseStatementNode ifElseStatementNode =
                createIfElseStatementNode(ifKeyWord, mainCondition, ifBlockStatementMain, null);
        foreachBlock.add(ifElseStatementNode);
        BlockStatementNode forEachBlockStatement = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(foreachBlock), createToken(CLOSE_BRACE_TOKEN));

        ForEachStatementNode forEachStatementNode = createForEachStatementNode(forEachKeyWord,
                typedBindingPatternNode, inKeyWord, actionOrExpr, forEachBlockStatement, null);

        statementNodes.add(forEachStatementNode);

        // return statement
        ExpressionStatementNode returnHeaderMap = getSimpleExpressionStatementNode("return headerMap");
        statementNodes.add(returnHeaderMap);

        FunctionBodyNode functionBodyNode = createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, createNodeList(statementNodes), createToken(CLOSE_BRACE_TOKEN));

        return createFunctionDefinitionNode(FUNCTION_DEFINITION, metadataNode, createEmptyNodeList(), functionKeyWord,
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);

    }
}
