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
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Client generation with filters.
 */
public class FilterTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/client").toAbsolutePath();
    SyntaxTree syntaxTree;
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();


    @Test(description = "With tag filter")
    public void testWithTag() throws IOException, BallerinaOpenApiException {
        Path definitionPath = RES_DIR.resolve("file_provider/swagger/tag.yaml");
        Path expectedPath = RES_DIR.resolve("file_provider/ballerina/tag.bal");
        list1.clear();
        list2.clear();
        list1.add("Data for all countries");
        Filter filter = new Filter(list1, list2);
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        //compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @Test(description = "With Operation filter")
    public void testWithOperation() throws IOException, BallerinaOpenApiException, FormatterException {
        Path definitionPath = RES_DIR.resolve("file_provider/swagger/operation.yaml");
        Path expectedPath = RES_DIR.resolve("file_provider/ballerina/operation.bal");
        list1.clear();
        list2.clear();
        list2.add("getCountryList");
        Filter filter = new Filter(list1, list2);
        syntaxTree = BallerinaClientGenerator.generateSyntaxTree(definitionPath, filter);
        //compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

}
