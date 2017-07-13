/*
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.tslint.services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author andreas
 */
public abstract class JsonBasedLinterClient extends AbstractLinterClient {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String ERROR_PREFIX = "ERROR: ";
    private static final String RESULT_PREFIX = "RESULT: ";

    private ObjectMapper mapper;
    private BufferedReader input;
    private PrintWriter output;

    public JsonBasedLinterClient(BufferedReader input, PrintWriter output) {
        this.input = input;
        this.output = output;

        this.mapper = new ObjectMapper();
    }

    @Override
    public synchronized <T> T call(Request request, JavaType resultType) {
        checkNotNull(request);
        checkNotNull(resultType);

        // process the request
        String resultJson;
        try {
            String requestJson = this.mapper.writeValueAsString(request);

            resultJson = this.processRequest(requestJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // convert the JSON result into a Java object
        try {
            return this.mapper.readValue(resultJson, resultType);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing result: " + resultJson, e);
        }
    }

    protected String processRequest(String requestJson) throws IOException {
        checkNotNull(requestJson);

        // write the request JSON to the bridge's stdin
        this.output.println(requestJson);
        this.output.println();
        this.output.flush();

        // read the response JSON from the bridge's stdout
        return read(this.input);
    }

    protected String read(BufferedReader reader) throws IOException {

        String resultJson = null;
        do {

            String line = reader.readLine();

            // process errors and logger statements
            if (line == null) {
                // restart bridge
                throw new NodeFuckedUpException(null);
            } else if (line.startsWith(ERROR_PREFIX)) {
                // remove prefix
                line = line.substring(ERROR_PREFIX.length(), line.length());
                // put newlines back
                line = line.replaceAll("\\\\n", LINE_SEPARATOR); // put newlines back
                // replace soft tabs with hardtabs to match Java's error stack trace.
                line = line.replaceAll("    ", "\t");

                throw new RuntimeException("The following request caused an error to be thrown:" + LINE_SEPARATOR
                        + line);
            } else if (line.startsWith(RESULT_PREFIX)) {
                resultJson = line.substring(RESULT_PREFIX.length());
            } else { // log statement
                System.out.println(line);
            }

        } while (resultJson == null);

        return resultJson;
    }

    @Override
    public void dispose() {
        super.dispose();

        try {
            this.input.close();
        } catch (IOException e) {
            // nothing to do
        }

        this.output.close();
    }

}
