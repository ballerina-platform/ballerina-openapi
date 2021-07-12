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
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.openapi.cmd.Filter;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.generators.BallerinaClientGenerator.generateSyntaxTree;
import static io.ballerina.generators.BallerinaClientGenerator.getPathParameters;
import static io.ballerina.generators.BallerinaClientGenerator.getQueryParameters;
import static io.ballerina.generators.common.TestUtils.getDiagnostics;
import static io.ballerina.generators.common.TestUtils.getOpenAPI;

/**
 * This tests class for the tests Query parameters in swagger file.
 */
public class QueryParameterTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/client").toAbsolutePath();
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();
    Filter filter = new Filter(list1, list2);

    @Test(description = "Generate Client for query parameter has default value")
    public void generateQueryParamwithDefault() throws IOException, BallerinaOpenApiException, FormatterException {
        Path definitionPath = RES_DIR.resolve("swagger/query_param_with_default_value.yaml");
        Path generatedPath = RES_DIR.resolve("ballerina/query_param_with_default_value.bal");
        SyntaxTree syntaxTree = generateSyntaxTree(definitionPath, filter);
        List<Diagnostic> diagnostics = getDiagnostics(definitionPath, syntaxTree);
        Assert.assertTrue(diagnostics.isEmpty());
        //compareGeneratedSyntaxTreeWithExpectedSyntaxTree(generatedPath, syntaxTree);
    }

    @Test(description = "Generate Client for query parameter has default value")
    public void testQueryParamFunction() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("swagger/query_param_with_default_value.yaml");
        OpenAPI openAPI = getOpenAPI(definitionPath);
        Parameter parameter = openAPI.getPaths().get("/onecall").getGet().getParameters().get(3);
        Node queryParameters = getQueryParameters(parameter);
        String expected = "@display{label:\"Units\"}int?units=12";
        Assert.assertEquals(queryParameters.toString(), expected);

        Parameter parameter02 = openAPI.getPaths().get("/onecall").getGet().getParameters().get(4);
        Node qParameter02 = getQueryParameters(parameter02);
        String eParameter02 = "@display{label:\"Language\"}string?lang=()";
        Assert.assertEquals(qParameter02.toString(), eParameter02);
    }

    @Test(description = "When path parameter has given unmatch data type in ballerina",
            expectedExceptions = BallerinaOpenApiException.class,
            expectedExceptionsMessageRegExp = "Invalid path parameter data type for the parameter: .*")
    public void testInvalidPathParameterType() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("swagger/path_parameter.yaml");
        OpenAPI openAPI = getOpenAPI(definitionPath);
        Parameter parameter = openAPI.getPaths().get("/v1/{id}").getGet().getParameters().get(0);
        // int, string, boolean, decimal, float,
        Node pathParameters = getPathParameters(parameter);
    }
}
