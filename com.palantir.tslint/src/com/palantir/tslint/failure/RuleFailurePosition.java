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

public final class RuleFailurePosition {
	private int character;
	private int line;
	private int position;

	public RuleFailurePosition(@JsonProperty("character") int character,
			@JsonProperty("line") int line,
			@JsonProperty("position") int position) {
		this.character = character;
		this.line = line;
		this.position = position;
	}

	public int getCharacter() {
		return this.character;
	}

	public int getLine() {
		return this.line;
	}

	public int getPosition() {
		return this.position;
	}

    @Override
    public String toString() {
        return "pos [char=" + this.character + ", " + this.line + ":" + this.position + "]";
    }


}
