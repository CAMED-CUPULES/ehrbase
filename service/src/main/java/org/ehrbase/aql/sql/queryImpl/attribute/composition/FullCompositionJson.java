/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryImpl.attribute.composition;

import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.GenericJsonPath;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;

import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.aql.sql.queryImpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryImpl.AqlRoutines.jsonpathParameters;
import static org.ehrbase.jooq.pg.Routines.jsComposition2;
import static org.ehrbase.jooq.pg.Tables.COMPOSITION;

public class FullCompositionJson extends CompositionAttribute {

    protected TableField tableField = COMPOSITION.ID;
    protected Optional<String> jsonPath = Optional.empty();

    public FullCompositionJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        fieldContext.setRmType("COMPOSITION");
        //to retrieve DB dialect
        Configuration configuration = fieldContext.getContext().configuration();

        //query the json representation of EVENT_CONTEXT and cast the result as TEXT
        Field jsonFullComposition;

        //TODO: add missing UC test
        if (jsonPath.isPresent()) {
            jsonFullComposition = DSL.field(
                    jsonpathItemAsText(configuration,
                        jsComposition2(
                                DSL.field(I_JoinBinder.compositionRecordTable.getName()+"."+tableField.getName()).cast(UUID.class),
                                DSL.val(fieldContext.getServerNodeId())
                        ).cast(JSONB.class),
                        jsonpathParameters(jsonPath.get())
                    )
            );
        }
        else
            jsonFullComposition = DSL.field(
                    jsComposition2(
                        DSL.field(I_JoinBinder.compositionRecordTable.getName()+"."+tableField.getName()).cast(UUID.class),
                        DSL.val(fieldContext.getServerNodeId())
                    ).cast(String.class)
            );

        if (fieldContext.isWithAlias())
            return aliased(DSL.field(jsonFullComposition));
        else
            return DSL.field(jsonFullComposition).as(fieldContext.getIdentifier());
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }

    public FullCompositionJson forJsonPath(String jsonPath){
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(new GenericJsonPath(jsonPath).jqueryPath());
        return this;
    }
}

