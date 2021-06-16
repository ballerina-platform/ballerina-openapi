/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.ballerina;

import io.ballerina.ballerina.service.ConverterConstants;
import io.ballerina.ballerina.service.OpenApiEndpointMapper;
import io.ballerina.ballerina.service.OpenApiServiceMapper;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.ballerina.service.OpenApiEndpointMapper.extractServerForExpressionNode;
import static io.ballerina.ballerina.service.OpenApiEndpointMapper.getServiceBasePath;
import static io.ballerina.openapi.utils.CodegenUtils.writeFile;

/**
 * OpenApi related utility classes.
 */

public class OpenApiConverterUtils {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiConverterUtils.class);
    private static SyntaxTree syntaxTree;
    private static SemanticModel semanticModel;
    private static Project project;
    private static List<ListenerDeclarationNode> endpoints = new ArrayList<>();

    /**
     * This util for generating files when not available with specific service name.
     *
     * @param servicePath The path to a single ballerina file
     * @param outPath     The output directory to which the OpenAPI specifications should be generated to.
     * @param serviceName Filter the services to generate OpenAPI specification for service with this name.
     * @throws IOException               Error when writing the OpenAPI specification file.
     * @throws OpenApiConverterException Error occurred generating OpenAPI specification.
     */
    public static void generateOAS3DefinitionsAllService(Path servicePath, Path outPath, Optional<String> serviceName
            , Boolean needJson)
            throws IOException, OpenApiConverterException {
        endpoints.clear();
        List<String> availableService = new ArrayList<>();

        // Load project instance for single ballerina file
        try {
            project = ProjectLoader.loadProject(servicePath);
        } catch (ProjectException e) {
            logger.error("Error while generating project instance:" + e);
            return;
        }

        //Travers and filter service
        //Take package name for project
        Package packageName = project.currentPackage();
        List<ServiceDeclarationNode> servicesToGenerate = new ArrayList<>();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            docId = project.documentId(servicePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            // Take module instance for traversing the syntax tree
            Module currentModule = packageName.getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

            docId = documentIterator.next();
            doc = currentModule.document(docId);
        }
        syntaxTree = doc.syntaxTree();
        semanticModel =  project.currentPackage().getCompilation().getSemanticModel(docId.moduleId());
        if (!semanticModel.diagnostics().isEmpty()) {
            throw new OpenApiConverterException("Given ballerina file has syntax/compilation error.");
        } else {
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            for (Node node : modulePartNode.members()) {
                SyntaxKind syntaxKind = node.kind();
                // Load a listen_declaration for the server part in the yaml spec
                if (syntaxKind.equals(SyntaxKind.LISTENER_DECLARATION)) {
                    ListenerDeclarationNode listener = (ListenerDeclarationNode) node;
                    endpoints.add(listener);
                }
                // Load a service Node
                if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                    ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                    if (serviceName.isPresent()) {
                        // Filtering by service name
                        String service = getServiceBasePath(serviceNode);
                        availableService.add(service);
                        if (serviceName.get().equals(service)) {
                            servicesToGenerate.add(serviceNode);
                        }
                    } else {
                        // To generate for all services
                        servicesToGenerate.add(serviceNode);
                    }
                }
            }

            // If there are no services found for a given service name.
            if (serviceName.isPresent() && servicesToGenerate.isEmpty()) {
                throw new OpenApiConverterException("No Ballerina services found with name '" + serviceName.get() +
                        "' to generate an OpenAPI specification. These services are " +
                        "available in ballerina file. " + availableService.toString());
            }

            // Generating for the services
            for (ServiceDeclarationNode serviceNode : servicesToGenerate) {
                String serviceNodeName = getServiceBasePath(serviceNode);
                String openApiName = getOpenApiFileName(syntaxTree.filePath(), serviceNodeName, needJson);
                String openApiSource = generateOAS3Definitions(syntaxTree, serviceNodeName, needJson);
                //  Checked old generated file with same name
                openApiName = checkDuplicateFiles(outPath, openApiName, needJson);
                writeFile(outPath.resolve(openApiName), openApiSource);
            }
        }
    }

    private static String getOpenApiFileName(String servicePath, String serviceName, Boolean isJson) {
        String cleanedServiceName;
        if (serviceName.isBlank() || serviceName.equals("/")) {
            cleanedServiceName = FilenameUtils.removeExtension(servicePath);
        } else {
            // Remove starting path separate if exists
            if (serviceName.startsWith("/")) {
                serviceName = serviceName.substring(1);
            }

            // Replace rest of the path separators with hyphen
            cleanedServiceName = serviceName.replaceAll("/", "-");
        }
        if (isJson) {
            return cleanedServiceName + ConverterConstants.OPENAPI_SUFFIX + ConverterConstants.JSON_EXTENSION;
        }
        return cleanedServiceName + ConverterConstants.OPENAPI_SUFFIX + ConverterConstants.YAML_EXTENSION;
    }

    public static String generateOAS3Definitions(SyntaxTree ballerinaSource, String serviceName, Boolean needJson) {
        //travers syntax tree
        //check top level node for get the annotation attachment for openapi

        //If no annotations are defined, assume it's not generated by any command and proceed with
        //just compile to get OpenApi JSON

        ModulePartNode modulePartNode = ballerinaSource.rootNode();
        for (Node node : modulePartNode.members()) {
            SyntaxKind syntaxKind = node.kind();
            if (syntaxKind.equals(SyntaxKind.ANNOTATION_DECLARATION)) {
                // TO-Do

            } else if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                OpenApiServiceMapper openApiServiceMapper = new OpenApiServiceMapper();
                openApiServiceMapper.setSemanticModel(semanticModel);
                OpenAPI openapi = getOpenApiDefinition(new OpenAPI(), openApiServiceMapper, serviceName, endpoints);
                if (needJson) {
                    return Json.pretty(openapi);
                }
                return Yaml.pretty(openapi);
            }
        }
        return serviceName;
    }

    private static OpenAPI getOpenApiDefinition(OpenAPI openapi, OpenApiServiceMapper openApiServiceMapper,
                                                String serviceName, List<ListenerDeclarationNode> endpoints) {
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        for (Node node : modulePartNode.members()) {
            SyntaxKind syntaxKind = node.kind();
            // Load a listen_declaration for the server part in the yaml spec
            if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                ServiceDeclarationNode serviceDefinition = (ServiceDeclarationNode) node;
                //Take base path of service
                String currentServiceName = getServiceBasePath(serviceDefinition);
                if (openapi.getServers() == null) {
                    OpenApiEndpointMapper openApiEndpointMapper = new OpenApiEndpointMapper();
                    SeparatedNodeList<ExpressionNode> expressions = ((ServiceDeclarationNode) node).expressions();
                    openapi = extractServerForExpressionNode(openapi, expressions, serviceDefinition);
                    if (!endpoints.isEmpty()) {
                        openapi = openApiEndpointMapper.convertListenerEndPointToOpenAPI(openapi, endpoints,
                                serviceDefinition);
                    }

                    if (openapi.getServers().isEmpty()) {
                        List<Server> servers = new ArrayList<>();
                        Server server = new Server().url(currentServiceName);
                        servers.add(server);
                        openapi.setServers(servers);
                    }
                    // Generate openApi string for the mentioned service name.
                    if (!serviceName.isBlank()) {
                        if (currentServiceName.trim().equals(serviceName)) {
                            openapi = openApiServiceMapper.convertServiceToOpenApi(serviceDefinition, openapi,
                                    serviceName);
                        }
                    } else {
                    // If no service name mentioned, then generate openApi definition for the first service.
                    openapi = openApiServiceMapper.convertServiceToOpenApi(serviceDefinition, openapi,
                            currentServiceName.trim());
                    }
                }
            }
        }

        return openapi;
    }


    public static Property mapBallerinaTypes(String type, boolean isArray) {
        switch (type) {
            case "any":
                //TODO handle any type to OpenApi
                return null;
            case "int":
                return new IntegerProperty();
            case "string":
                return new StringProperty();
            case "boolean":
                return new BooleanProperty();
            case "decimal":
                return new DecimalProperty();
            case "byte":
                return new ByteArrayProperty();
            case "float":
                return new FloatProperty();
            case "json":
                //TODO json is mapped to Object property. Will need to handle it properly.
                return new ObjectProperty();
            default:
                //TODO handle unmatched type
                return null;
        }
    }

    /**
     * This method use for checking the duplicate files.
     * @param outPath       output path for file generated
     * @param openApiName   given file name
     * @return              file name with duplicate number tag
     */
    private static String checkDuplicateFiles(Path outPath, String openApiName, Boolean isJson) {

        if (Files.exists(outPath)) {
            final File[] listFiles = new File(String.valueOf(outPath)).listFiles();
            if (listFiles != null) {
                openApiName = checkAvailabilityOfGivenName(openApiName, listFiles, isJson);
            }
        }
        return openApiName;
    }

    private static String checkAvailabilityOfGivenName(String openApiName, File[] listFiles, Boolean isJson) {

        for (File file : listFiles) {
            if (file.getName().equals(openApiName)) {
                if (System.console() != null) {
                    String userInput = System.console().readLine("There is already a/an " + file.getName() +
                            " in the location. Do you want to override the file? [y/N] ");
                    if (!Objects.equals(userInput.toLowerCase(Locale.ENGLISH), "y")) {
                        int duplicateCount = 0;
                        openApiName = setGeneratedFileName(listFiles, openApiName, duplicateCount, isJson);
                    }
                }
            }
        }
        return openApiName;
    }

    /**
     *  This method for setting the file name for generated file.
     * @param listFiles         generated files
     * @param fileName          File name
     * @param duplicateCount    add the tag with duplicate number if file already exist
     */
    private static String setGeneratedFileName(File[] listFiles, String fileName, int duplicateCount, Boolean isJson) {
        for (File listFile : listFiles) {
            String listFileName = listFile.getName();
            if (listFileName.contains(".") && ((listFileName.split("\\.")).length >= 2)
                    && (listFileName.split("\\.")[0]
                    .equals(fileName.split("\\.")[0]))) {
                duplicateCount = 1 + duplicateCount;
            }
        }
        if (isJson) {
            return fileName.split("\\.")[0] + "." + (duplicateCount) + ".json";
        }
        return fileName.split("\\.")[0] + "." + (duplicateCount) + ".yaml";
    }
}
