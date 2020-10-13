# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Jake Smolka (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*
Metadata    Author    *Jake Smolka*
Metadata    Created    2020.09.01

Metadata        TOP_TEST_SUITE    ADMIN_EHR
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     startup SUT
Suite Teardown  shutdown SUT

Force Tags     composition



*** Test Cases ***

ADMIN - Delete Composition
    # pre check
    Connect With DB
    check composition admin delete table counts
    # preparing and provisioning
    upload OPT    minimal/minimal_observation.opt
    prepare new request session    JSON    Prefer=return=representation
    create supernew ehr
    Set Test Variable  ${ehr_id}  ${response.body.ehr_id.value}
    ehr_keywords.validate POST response - 201 created ehr
    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml
    # Execute admin delete EHR
    admin delete composition
    Log To Console  ${response}
    # Test with count rows again - post check
    check composition admin delete table counts



*** Keywords ***

startup SUT
    [Documentation]     Overrides `generic_keywords.startup SUT` keyword
    ...                 to add some ENVs required by this test suite.

    Set Environment Variable    ADMINAPI_ACTIVE    true
    Set Environment Variable    SYSTEM_ALLOWTEMPLATEOVERWRITE    true
    generic_keywords.startup SUT


admin delete composition
    [Documentation]     Admin delete of Composition.
    ...                 Needs `${versioned_object_uid}` var from e.g. `commit composition (JSON)` KW.

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}/composition/${versioned_object_uid}
                        Should Be Equal As Strings   ${resp.status}   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


check composition admin delete table counts

    # TODO: could the target number of rows calculated, e.g. new audits = old - 1 ?
    # ${contr_records}=   Count Rows In DB Table    ehr.contribution
    #                     Should Be Equal As Integers    ${contr_records}     ${0}
    # ${contr_h_records}=   Count Rows In DB Table    ehr.contribution_history
    #                     Should Be Equal As Integers    ${contr_h_records}     ${0}
    # ${audit_records}=   Count Rows In DB Table    ehr.audit_details
    #                     Should Be Equal As Integers    ${audit_records}     ${0}
    # ${system_records}=   Count Rows In DB Table    ehr.system
    #                     Should Be Equal As Integers    ${system_records}     ${0}
    #${party_records}=   Count Rows In DB Table    ehr.party_identified
    #                    Should Be Equal As Integers    ${party_records}     ${0}
    ${compo_records}=   Count Rows In DB Table    ehr.composition
                        Should Be Equal As Integers    ${compo_records}     ${0}
    ${compo_h_records}=  Count Rows In DB Table    ehr.composition_history
                        Should Be Equal As Integers    ${compo_h_records}     ${0}
    ${entry_records}=   Count Rows In DB Table    ehr.entry
                        Should Be Equal As Integers    ${entry_records}     ${0}
    ${entry_h_records}=  Count Rows In DB Table    ehr.entry_history
                        Should Be Equal As Integers    ${entry_h_records}     ${0}
    ${event_context_records}=   Count Rows In DB Table    ehr.event_context
                        Should Be Equal As Integers    ${event_context_records}     ${0}
    ${entry_participation_records}=   Count Rows In DB Table    ehr.participation
                        Should Be Equal As Integers    ${entry_participation_records}     ${0}