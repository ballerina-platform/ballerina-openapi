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

import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.generators.BallerinaClientGenerator;
import io.ballerina.openapi.cmd.Filter;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.generators.BallerinaClientGenerator.getFunctionBodyNode;
import static io.ballerina.generators.common.TestUtils.getOpenAPI;

/**
 * All the tests related to the Header sections in the swagger file.
 */
public class HeadersTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/client").toAbsolutePath();
    private SyntaxTree syntaxTree;
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();
    Filter filter = new Filter(list1, list2);

    @Test(description = "Test for header that comes under the parameter section.")
    public void getHeaderTests() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("diagnostic_files/header_parameter.yaml");
        OpenAPI display = getOpenAPI(definitionPath);
        Set<Map.Entry<PathItem.HttpMethod, Operation>> operation =
                display.getPaths().get("/pets").readOperationsMap().entrySet();
        Iterator<Map.Entry<PathItem.HttpMethod, Operation>> iterator = operation.iterator();
        FunctionBodyNode bodyNode = getFunctionBodyNode("/pets", iterator.next());
        Assert.assertEquals(bodyNode.toString().trim().replaceAll("\\s+", ""), ("{string path= " +
                "string`/pets`; map<any>headerValues={\"X-Request-ID\":xRequestId," +
                "\"X-Request-Client\":xRequestClient};map<string|string[]>accHeaders=getMapForHeaders(headerValues);" +
                "_=checkself.clientEp->get(path,accHeaders,targetType=http:Response);}").trim()
                .replaceAll("\\s+", ""));
    }

    @Test(description = "Tests for full structure in header")
    public void getHeader() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("diagnostic_files/header_parameter.yaml");
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        Path expectedPath = RES_DIR.resolve("ballerina/header_parameter.bal");
        //compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }
}
