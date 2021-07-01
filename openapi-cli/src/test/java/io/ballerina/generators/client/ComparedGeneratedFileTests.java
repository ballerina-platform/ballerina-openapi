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

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.generators.BallerinaClientGenerator;
import io.ballerina.openapi.cmd.Filter;
import io.ballerina.openapi.exception.BallerinaOpenApiException;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;
import static io.ballerina.generators.common.TestUtils.getDiagnostics;

/**
 * All the tests related to the {@link io.ballerina.generators.BallerinaClientGenerator} util.
 */
public class ComparedGeneratedFileTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/client").toAbsolutePath();
    private static final Path clientPath = RES_DIR.resolve("ballerina_project/client.bal");
    private static final Path schemaPath = RES_DIR.resolve("ballerina_project/types.bal");
    SyntaxTree syntaxTree;
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();
    Filter filter = new Filter(list1, list2);

    @Test(description = "Generate Client for path parameter has parameter name as key word", enabled = false)
    public void generateClientForJira() throws IOException, BallerinaOpenApiException, FormatterException {
//        Path definitionPath = RES_DIR.resolve("swagger/return_type/inline_all_of_response.yaml");
        Path definitionPath = RES_DIR.resolve("file_provider/swagger/uber_openapi.yaml");
        Path expectedPath = RES_DIR.resolve("file_provider/ballerina/jira_openapi.bal");
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        List<Diagnostic> diagnostics = getDiagnostics(definitionPath, syntaxTree);
        Assert.assertTrue(diagnostics.isEmpty());
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @Test(description = "Generate Client for salesforce yaml", enabled = false)
    public void generateClientForSalesForce() throws IOException, BallerinaOpenApiException, FormatterException {
        Path definitionPath = RES_DIR.resolve("swagger/salesforce.yaml");
        Path expectedPath = RES_DIR.resolve("/ballerina/salesforce.bal");
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        List<Diagnostic> diagnostics = getDiagnostics(definitionPath, syntaxTree);
        Assert.assertTrue(diagnostics.isEmpty());
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @Test(description = "Test openAPI definition to ballerina client source code generation",
            dataProvider = "fileProviderForFilesComparison")
    public void  openApiToBallerinaCodeGenTestForClient(String yamlFile, String expectedFile) throws IOException,
            BallerinaOpenApiException, FormatterException {
        Path definitionPath = RES_DIR.resolve("file_provider/swagger/" + yamlFile);
        Path expectedPath = RES_DIR.resolve("file_provider/ballerina/" + expectedFile);
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        List<Diagnostic> diagnostics = getDiagnostics(definitionPath, syntaxTree);
        Assert.assertTrue(diagnostics.isEmpty());
        //compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @DataProvider(name = "fileProviderForFilesComparison")
    public Object[][] fileProviderForFilesComparison() {
        return new Object[][]{
                {"openapi_weather_api.yaml", "openapi_weather_api.bal"},
                {"multiple_pathparam.yaml", "multiple_pathparam.bal"}
        };
    }

    @AfterTest
    private void deleteGeneratedFiles() {
        try {
            Files.deleteIfExists(clientPath);
            Files.deleteIfExists(schemaPath);
        } catch (IOException e) {
            //Ignore the exception
        }
    }
}
