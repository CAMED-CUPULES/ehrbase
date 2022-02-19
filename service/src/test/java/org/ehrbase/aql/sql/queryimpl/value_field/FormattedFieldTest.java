/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl.value_field;

import org.ehrbase.aql.TestAqlBase;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.junit.Assert.*;

import org.assertj.core.api.AbstractStringAssert;

public class FormattedFieldTest<equalToIgnoringWhitespace> extends TestAqlBase {

    /**
     *
     */
    private static final AbstractStringAssert<SELF> AS = assertThat(DSL.select(field).getQuery().toString())
            .as("test formatting dvdatetime value");
    FieldResolutionContext fieldResolutionContext;
    JoinSetup joinSetup = new JoinSetup();
    private AbstractStringAssert<SELF> equalToIgnoringWhitespace;

    @Before
    public void setUp() {
        fieldResolutionContext = new FieldResolutionContext(
                testDomainAccess.getContext(),
                "test",
                "test",
                new VariableDefinition("test", null, "test", false),
                IQueryImpl.Clause.SELECT,
                null,
                testDomainAccess.getIntrospectService(),
                null);
    }

    @Test
    public <equalToIgnoringWhitespace> void testSelectEhrDateCreatedValue() {
        var field = new FormattedField(fieldResolutionContext, joinSetup)
                .usingToJson("timestamp with time zone", "||", JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED),
                        JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED_TZID));

        assertNotNull(field);
        equalToIgnoringWhitespace = AS
                .isEqualToIgnoringWhitespace(
                        "select cast(to_json(cast(\"ehr_join\".\"date_created\"||\"ehr_join\".\"date_created_tzid\" as varchar)) as jsonb) \"/test\"");
    }

}