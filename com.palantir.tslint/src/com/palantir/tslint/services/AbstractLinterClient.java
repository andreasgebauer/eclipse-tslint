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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author andreas
 */
public abstract class AbstractLinterClient implements LinterClient {

    @Override
    public <T> T call(Request request, Class<T> resultType) {
        checkNotNull(request);
        checkNotNull(resultType);

        JavaType type = TypeFactory.defaultInstance().uncheckedSimpleType(resultType);

        return this.call(request, type);
    }

    @Override
    public void dispose() {

    }

    protected abstract <T> T call(Request request, JavaType resultType);

}
