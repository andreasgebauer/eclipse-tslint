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

package com.palantir.tslint.failure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author andreas
 */
public class Fix {
    private int innerStart;
    private int innerLength;
    private String innerText;

    public Fix(@JsonProperty("innerStart") int innerStart,
            @JsonProperty("innerLength") int innerLength,
            @JsonProperty("innerText") String innerText) {
        this.innerStart = innerStart;
        this.innerLength = innerLength;
        this.innerText = innerText;
    }

    public int getInnerStart() {
        return this.innerStart;
    }

    public int getInnerLength() {
        return this.innerLength;
    }

    public String getInnerText() {
        return this.innerText;
    }

}
