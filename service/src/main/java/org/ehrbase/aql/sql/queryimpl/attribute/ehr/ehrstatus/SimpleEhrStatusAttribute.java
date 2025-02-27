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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import static org.ehrbase.jooq.pg.Tables.STATUS;

@SuppressWarnings({"java:S3740","java:S1452"})
public class SimpleEhrStatusAttribute extends EhrStatusAttribute {

    protected Field tableField;

    public SimpleEhrStatusAttribute(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        return as(DSL.field(tableField));
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        if (tableField.getTable().equals(STATUS)) {
            joinSetup.setJoinEhrStatus(true);
            this.tableField = JoinBinder.statusRecordTable.field(tableField.getName());
        }
        return this;
    }
}
