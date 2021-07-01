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

package io.ballerina.generators.auth;

import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.generators.GeneratorConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createDefaultableParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.generators.GeneratorConstants.API_KEY;
import static io.ballerina.generators.GeneratorConstants.API_KEY_CONFIG;
import static io.ballerina.generators.GeneratorConstants.API_KEY_CONFIG_PARAM;
import static io.ballerina.generators.GeneratorConstants.API_KEY_CONFIG_RECORD_FIELD;
import static io.ballerina.generators.GeneratorConstants.API_KEY_MAP;
import static io.ballerina.generators.GeneratorConstants.AUTH_CONFIG_FILED_NAME;
import static io.ballerina.generators.GeneratorConstants.AuthConfigTypes;
import static io.ballerina.generators.GeneratorConstants.BASIC;
import static io.ballerina.generators.GeneratorConstants.BEARER;
import static io.ballerina.generators.GeneratorConstants.CONFIG_RECORD_ARG;
import static io.ballerina.generators.GeneratorConstants.CONFIG_RECORD_NAME;
import static io.ballerina.generators.GeneratorConstants.HTTP;
import static io.ballerina.generators.GeneratorConstants.OAUTH2;
import static io.ballerina.generators.GeneratorConstants.SSL_FIELD_NAME;
import static io.ballerina.generators.GeneratorUtils.escapeIdentifier;

/**
 * This class is used to generate authentication related nodes of the ballerina connector client syntax tree.
 */
public class BallerinaAuthConfigGenerator {
    private static final List<String> headerApiKeyNameList = new ArrayList<>();
    private static final List<String> queryApiKeyNameList = new ArrayList<>();
    private static boolean isAPIKey = false;
    private static boolean isHttpOROAuth = false;

    /**
     * Generate the Config record for the relevant authentication type.
     * -- ex: Config record for Http and OAuth 2.0 Authentication mechanisms.
     * <pre>
     *     public type ClientConfig record {
     *          http:CredentialsConfig authConfig;
     *          http:ClientSecureSocket secureSocketConfig?;
     *     };
     * </pre>
     * -- ex: Config record for API Key Authentication mechanism.
     * <pre>
     *     public type ApiKeysConfig record {
     *          map<string|string[]> apiKeys;
     *     };
     * </pre>
     *
     * @param openAPI                       OpenApi object received from swagger open-api parser
     * @return {@link TypeDefinitionNode}   Synatx tree node of config record
     */
    public static TypeDefinitionNode getConfigRecord (OpenAPI openAPI) {
        clearStaticVariables();
        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
            List<Node> recordFieldList = addItemstoRecordFieldList(openAPI);
            if (!recordFieldList.isEmpty()) {
                Token typeName;
                if (isHttpOROAuth) {
                    typeName = AbstractNodeFactory.createIdentifierToken(CONFIG_RECORD_NAME);
                } else {
                    typeName = AbstractNodeFactory.createIdentifierToken(API_KEY_CONFIG);
                }
                Token visibilityQualifierNode = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.PUBLIC);
                Token typeKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.TYPE);
                Token recordKeyWord = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.RECORD);
                Token bodyStartDelimiter = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.OPEN_BRACE);
                NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFieldList);
                Token bodyEndDelimiter = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.CLOSE_BRACE);
                RecordTypeDescriptorNode recordTypeDescriptorNode =
                        NodeFactory.createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter,
                                fieldNodes, null, bodyEndDelimiter);
                Token semicolon = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.SEMICOLON);
                return NodeFactory.createTypeDefinitionNode(null,
                        visibilityQualifierNode, typeKeyWord, typeName, recordTypeDescriptorNode, semicolon);
            }
        }
        return null;
    }

    /**
     * Generate Class variable for api key map {@code map<string|string[]> apiKeys; }.
     *
     * @return {@link List<ObjectFieldNode>}    syntax tree object field node list
     */
    public static ObjectFieldNode getApiKeyMapClassVariable() { // return ObjectFieldNode
        if (!isHttpOROAuth && isAPIKey) {
            NodeList<Token> qualifierList = createEmptyNodeList();
            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(API_KEY_MAP));
            IdentifierToken fieldName = createIdentifierToken(API_KEY_CONFIG_RECORD_FIELD);
            MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
            return createObjectFieldNode(metadataNode, null,
                    qualifierList, typeName, fieldName, null, null, createToken(SEMICOLON_TOKEN));
        }
        return null;
    }

    /**
     * Generate the config parameters of the client class init method.
     * -- ex: Config param for Http and OAuth 2.0 Authentication mechanisms.
     *        {@code ClientConfig clientConfig }
     * -- ex: Config param for API Key Authentication mechanism.
     *        {@code ApiKeysConfig apiKeyConfig, http:ClientConfiguration clientConfig = {} }
     * -- ex: Config param for API Key Authentication mechanism.
     *        {@code http:ClientConfiguration clientConfig = {} }
     *
     * @return  {@link List<Node>}  syntax tree node list of config parameters
     */
    public static List<Node> getConfigParamForClassInit() {
        List<Node> parameters  = new ArrayList<>();
        IdentifierToken equalToken = createIdentifierToken(GeneratorConstants.EQUAL);
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        if (isHttpOROAuth) {
            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(CONFIG_RECORD_NAME));
            IdentifierToken paramName = createIdentifierToken(CONFIG_RECORD_ARG);
            RequiredParameterNode authConfig = createRequiredParameterNode(annotationNodes, typeName, paramName);
            parameters.add(authConfig);
        } else {
            if (isAPIKey) {
                BuiltinSimpleNameReferenceNode apiKeyConfigTypeName = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(API_KEY_CONFIG));
                IdentifierToken apiKeyConfigParamName = createIdentifierToken(API_KEY_CONFIG_PARAM);
                RequiredParameterNode apiKeyConfigParamNode = createRequiredParameterNode(annotationNodes,
                        apiKeyConfigTypeName, apiKeyConfigParamName);
                parameters.add(apiKeyConfigParamNode);
                parameters.add(createToken(COMMA_TOKEN));
            }
            BuiltinSimpleNameReferenceNode httpClientConfigTypeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken("http:ClientConfiguration"));
            IdentifierToken httpClientConfig = createIdentifierToken(CONFIG_RECORD_ARG);
            BasicLiteralNode emptyexpression = createBasicLiteralNode(null, createIdentifierToken(" {}"));
            DefaultableParameterNode defaultHTTPConfig = createDefaultableParameterNode(annotationNodes,
                    httpClientConfigTypeName,
                    httpClientConfig, equalToken, emptyexpression);
            parameters.add(defaultHTTPConfig);
        }
        return parameters;
    }

    /**
     * Generate {@code http:ClientSecureSocket secureSocketConfig } local variable.
     *
     * @return  {@link VariableDeclarationNode} syntax tree variable declaration node.
     */
    public static VariableDeclarationNode getSecureSocketInitNode () {
        if (isHttpOROAuth) {
            NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
            TypeDescriptorNode typeName = createOptionalTypeDescriptorNode(
                    createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken("http:ClientSecureSocket")), createToken(QUESTION_MARK_TOKEN));
            CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                    createIdentifierToken(SSL_FIELD_NAME));
            TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeName,
                    bindingPattern);
            ExpressionNode configRecordSSLField = createOptionalFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(CONFIG_RECORD_ARG)),
                    createIdentifierToken("?."),
                    createSimpleNameReferenceNode(createIdentifierToken(SSL_FIELD_NAME)));
            return createVariableDeclarationNode(annotationNodes,
                    null, typedBindingPatternNode, createToken(EQUAL_TOKEN), configRecordSSLField,
                    createToken(SEMICOLON_TOKEN));
        }
        return null;
    }

    /**
     * Generate http:client initialization node
     * -- ex: Config record for Http and OAuth 2.0 Authentication mechanisms.
     * <pre>
     *     http:Client httpEp = check new (serviceUrl, {
     *          auth: clientConfig.authConfig,
     *          secureSocket: secureSocketConfig
     *     });
     * </pre>
     * -- ex: Config record for API Key Authentication mechanism.
     * <pre>
     *     http:Client httpEp = check new (serviceUrl, clientConfig);
     * </pre>
     * * -- ex: Config record when no Security Schema is given
     * <pre>
     *     http:Client httpEp = check new (serviceUrl, clientConfig);
     * </pre>
     *
     * @return {@link VariableDeclarationNode}   Synatx tree node of client initialization
     */
    public static VariableDeclarationNode getClientInitializationNode () {
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        BuiltinSimpleNameReferenceNode typeBindingPattern = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken("http:Client"));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken("httpEp"));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                bindingPattern);

        //Expression node
        Token newKeyWord = createIdentifierToken("new");
        Token openParenArg = createIdentifierToken("(");
        List<Node> argumentsList = new ArrayList<>();
        PositionalArgumentNode positionalArgumentNode01 = createPositionalArgumentNode(createSimpleNameReferenceNode(
                createIdentifierToken(GeneratorConstants.SERVICE_URL)));
        argumentsList.add(positionalArgumentNode01);
        Token comma1 = createIdentifierToken(",");
        PositionalArgumentNode positionalArgumentNode02;
        if (isHttpOROAuth) {
            // try to create specific node
            positionalArgumentNode02 = createPositionalArgumentNode(createSimpleNameReferenceNode(
                    createIdentifierToken(String.format("{ auth: %s.%s, secureSocket: %s }", CONFIG_RECORD_ARG,
                            AUTH_CONFIG_FILED_NAME, SSL_FIELD_NAME))));
            argumentsList.add(comma1);
            argumentsList.add(positionalArgumentNode02);
        } else {
            positionalArgumentNode02 = createPositionalArgumentNode(createSimpleNameReferenceNode(
                    createIdentifierToken(CONFIG_RECORD_ARG)));
            argumentsList.add(comma1);
            argumentsList.add(positionalArgumentNode02);
        }
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        Token closeParenArg = createToken(CLOSE_PAREN_TOKEN);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenArg, arguments,
                closeParenArg);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(newKeyWord,
                parenthesizedArgList);
        CheckExpressionNode initializer = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                expressionNode);
        return createVariableDeclarationNode(annotationNodes,
                null, typedBindingPatternNode, createToken(EQUAL_TOKEN), initializer,
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generate assignment nodes for api key map assignment {@code self.apiKeys = apiKeyConfig.apiKeys;}.
     *
     * @return  {@link AssignmentStatementNode} syntax tree assignment statement node.
     */
    public static AssignmentStatementNode getApiKeyAssignmentNode() {
        if (!isHttpOROAuth && isAPIKey) {
            FieldAccessExpressionNode varRefApiKey = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken("self")), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG_RECORD_FIELD)));
            SimpleNameReferenceNode expr = createSimpleNameReferenceNode
                    (createIdentifierToken(API_KEY_CONFIG_PARAM +
                            GeneratorConstants.PERIOD + API_KEY_CONFIG_RECORD_FIELD));
            return createAssignmentStatementNode(varRefApiKey,
                    createToken(EQUAL_TOKEN), expr, createToken(SEMICOLON_TOKEN));
        }
        return null;
    }

    /**
     * Returns API Key names which need to send in the query string.
     *
     * @return  {@link List<String>}    API key name list
     */
    public static List<String> getQueryApiKeyNameList () {
        if (!isHttpOROAuth && isAPIKey) {
            return queryApiKeyNameList;
        }
        return new ArrayList<>();
    }

    /**
     * Returns API Key names which need to send as request headers.
     *
     * @return  {@link List<String>}    API key name list
     */
    public static List<String> getHeaderApiKeyNameList () {
        if (!isHttpOROAuth && isAPIKey) {
            return headerApiKeyNameList;
        }
        return new ArrayList<>();
    }

    /**
     * Return fields of config record for the given security schema.
     * -- ex: Record fields for Http and OAuth 2.0 Authentication mechanisms.
     * <pre>
     *      http:BearerTokenConfig|http:OAuth2RefreshTokenGrantConfig authConfig;
     *      http:ClientSecureSocket secureSocketConfig?;
     * </pre>
     * -- ex: Record fields for API Key Authentication mechanism.
     * <pre>
     *     map<string|string[]> apiKeys;
     * </pre>
     *
     * @return  {@link List<Node>}  syntax tree node list of record fields
     */
    private static List<Node> addItemstoRecordFieldList (OpenAPI openAPI) {
        List<Node> recordFieldNodes = new ArrayList<>();

        Token semicolonToken = AbstractNodeFactory.createIdentifierToken(GeneratorConstants.SEMICOLON);
        Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents().getSecuritySchemes();
        String httpFieldTypeNames = getConfigRecordFieldTypeNames (securitySchemeMap);
        if (!httpFieldTypeNames.isEmpty())  {
            // add auth config field
            Token authFieldType = AbstractNodeFactory.createIdentifierToken(httpFieldTypeNames);
            IdentifierToken authFieldName = AbstractNodeFactory.createIdentifierToken(escapeIdentifier(
                    AUTH_CONFIG_FILED_NAME));
            TypeDescriptorNode fieldTypeNode = createBuiltinSimpleNameReferenceNode(null, authFieldType);
            RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    fieldTypeNode, authFieldName, null, semicolonToken);
            recordFieldNodes.add(recordFieldNode);
            // add socket config
            IdentifierToken sslFieldNameNode = AbstractNodeFactory.createIdentifierToken(SSL_FIELD_NAME);
            TypeDescriptorNode sslfieldTypeNode = createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken("http:ClientSecureSocket"));
            RecordFieldNode sslRecordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    sslfieldTypeNode, sslFieldNameNode, createToken(QUESTION_MARK_TOKEN), semicolonToken);
            recordFieldNodes.add(sslRecordFieldNode);
        } else if (isAPIKey) {

            Token apiKeyMap = AbstractNodeFactory.createIdentifierToken(API_KEY_MAP);
            IdentifierToken apiKeyMapFieldName = AbstractNodeFactory.createIdentifierToken(API_KEY_CONFIG_RECORD_FIELD);
            TypeDescriptorNode fieldTypeNode = createBuiltinSimpleNameReferenceNode(null, apiKeyMap);
            RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    fieldTypeNode, apiKeyMapFieldName, null, semicolonToken);
            recordFieldNodes.add(recordFieldNode);
        }
        return recordFieldNodes;
    }

    /**
     * Travers through the security schemas of the given open api spec.
     * Store api key names which needs to send in the query string and as a request header separately.
     *
     * @param securitySchemeMap     Map of security schemas of the given open api spec
     * @return {@link String}       Type name of the authConfig field in ClientConfig record
     */
    private static String getConfigRecordFieldTypeNames(Map<String, SecurityScheme> securitySchemeMap) {
        Set<String> httpFieldTypeNames = new HashSet<String>();
        for (Map.Entry<String, SecurityScheme> securitySchemeEntry : securitySchemeMap.entrySet()) {
            SecurityScheme schemaValue = securitySchemeEntry.getValue();
            if (schemaValue != null && schemaValue.getType() != null) {
                String schemaType = schemaValue.getType().name().toLowerCase(Locale.getDefault());
                switch (schemaType) {
                    case HTTP:
                        isHttpOROAuth = true;
                        String scheme = schemaValue.getScheme();
                        if (scheme.equals(BASIC)) {
                            httpFieldTypeNames.add(AuthConfigTypes.BASIC.getValue());
                        } else if (scheme.equals(BEARER)) {
                            httpFieldTypeNames.add(AuthConfigTypes.BEARER.getValue());
                        }
                        break;
                    case OAUTH2:
                        isHttpOROAuth = true;
                        if (schemaValue.getFlows().getClientCredentials() != null) {
                            httpFieldTypeNames.add(AuthConfigTypes.CLIENT_CREDENTIAL.getValue());
                        }
                        if (schemaValue.getFlows().getPassword() != null) {
                            httpFieldTypeNames.add(AuthConfigTypes.PASSWORD.getValue());
                        }
                        if (schemaValue.getFlows().getAuthorizationCode() != null) {
                            httpFieldTypeNames.add(AuthConfigTypes.BEARER.getValue());
                            httpFieldTypeNames.add(AuthConfigTypes.REFRESH_TOKEN.getValue());
                        }
                        if (schemaValue.getFlows().getImplicit() != null) {
                            httpFieldTypeNames.add(AuthConfigTypes.BEARER.getValue());
                        }
                        break;
                    case API_KEY:
                        isAPIKey = true;
                        String apiKeyType = schemaValue.getIn().name().toLowerCase(Locale.getDefault());
                        switch (apiKeyType) {
                            case "query":
                                queryApiKeyNameList.add(schemaValue.getName());
                                break;
                            case "header":
                                headerApiKeyNameList.add(schemaValue.getName());
                                break;
                            default:
                                break;
                        }
                }
            }
        }
        return buildConfigRecordFieldTypes(httpFieldTypeNames).toString();
    }

    /**
     * This static method is used concat the config record authConfig field type.
     *
     * @param fieldtypes        Type name set from {@link #getConfigRecordFieldTypeNames(Map)} method.
     * @return {@link String}   Pipe concatenated list of type names
     */
    private static StringBuilder buildConfigRecordFieldTypes(Set<String> fieldtypes) {
        StringBuilder httpAuthFieldTypes = new StringBuilder();
        if (!fieldtypes.isEmpty()) {
            for (String fieldType: fieldtypes) {
                if (httpAuthFieldTypes.length() != 0) {
                    httpAuthFieldTypes.append("|").append(fieldType);
                } else {
                    httpAuthFieldTypes.append(fieldType);
                }
            }
        }
        return httpAuthFieldTypes;
    }

    /**
     * Clear class static variable at the beginning of execution.
     */
    private static void clearStaticVariables() {
        isHttpOROAuth = false;
        isAPIKey = false;
        queryApiKeyNameList.clear();
        headerApiKeyNameList.clear();
    }
}
