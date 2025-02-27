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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.ehrbase.aql.definition.LateralVariable;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

public class LateralJoins {

    private static int seed = 1;

    public void create(String templateId, TaggedStringBuilder encodedVar, I_VariableDefinition item,
            IQueryImpl.Clause clause) {
        var originalSqlExpression = encodedVar.toString();

        if (originalSqlExpression.isBlank())
            return;

        // check for existing lateral join in variable definition
        if (item.getLateralJoinDefinitions(templateId) != null
                && !item.getLateralJoinDefinitions(templateId).isEmpty()) {
            for (LateralJoinDefinition lateralJoinDefinition : item.getLateralJoinDefinitions(templateId)) {
                if (lateralJoinDefinition.getSqlExpression().equals(originalSqlExpression)) {
                    // use this definition instead of creating a new redundant one
                    item.setAlias(new LateralVariable(lateralJoinDefinition.getTable().getName(),
                            lateralJoinDefinition.getLateralVariable()).alias());
                    return;
                }
            }
        }

        int hashValue = encodedVar.toString().hashCode(); // cf. SonarLint
        int abs;
        if (hashValue != 0)
            abs = Math.abs(hashValue);
        else
            abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();
        // insert the variable alias used for the lateral join expression
        encodedVar.replaceLast(")", " AS " + variableAlias + ")");
        Table<Record> table = DSL.table(encodedVar.toString()).as(tableAlias);
        item.setLateralJoinTable(templateId,
                new LateralJoinDefinition(originalSqlExpression, table, variableAlias, JoinType.JOIN, null, clause));
        item.setAlias(new LateralVariable(tableAlias, variableAlias).alias());

    }

    public void create(String templateId, SelectQuery selectSelectStep, I_VariableDefinition item,
            IQueryImpl.Clause clause) {
        if (selectSelectStep == null)
            return;
        int hashValue = selectSelectStep.hashCode(); // cf. SonarLint
        int abs;
        if (hashValue != 0)
            abs = Math.abs(hashValue);
        else
            abs = 0;
        String tableAlias = "array_" + abs + "_" + inc();
        String variableAlias = "var_" + abs + "_" + inc();

        SelectSelectStep wrappedSelectSelectStep = DSL.select(DSL.field(selectSelectStep).as(variableAlias));

<<<<<<< HEAD
        SelectSelectStep wrappedSelectSelectStep2 = wrappedSelectSelectStep;
        Table<Record> table = DSL.table(wrappedSelectSelectStep2).as(tableAlias);
        item.setLateralJoinTable(templateId, new LateralJoinDefinition(selectSelectStep.getSQL(), table, variableAlias, JoinType.LEFT_OUTER_JOIN, DSL.condition(true), clause));
=======
        Table<Record> table = DSL.table(wrappedSelectSelectStep).as(tableAlias);
        item.setLateralJoinTable(templateId, new LateralJoinDefinition(selectSelectStep.getSQL(), table, variableAlias,
                JoinType.LEFT_OUTER_JOIN, DSL.condition(true), clause));
>>>>>>> e907ac15484d0562798c25e0bd59fed79a3356c3
        item.setSubstituteFieldVariable(variableAlias);
    }

    private static synchronized int inc() {
        return seed++;
    }
}
