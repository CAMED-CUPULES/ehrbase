/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.ehrscape.ContributionDto;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;
import org.ehrbase.response.openehr.VersionedObjectResponseData;
import org.ehrbase.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /ehr/{ehrId}/versioned_composition resource of openEHR REST API
 */
@Api
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr/{ehr_id}/versioned_composition", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrVersionedCompositionController extends BaseController {

    private final EhrService ehrService;
    private final CompositionService compositionService;
    private final ContributionService contributionService;

    @Autowired
    public OpenehrVersionedCompositionController(EhrService ehrService, CompositionService compositionService, ContributionService contributionService) {
        this.ehrService = Objects.requireNonNull(ehrService);
        this.compositionService = Objects.requireNonNull(compositionService);
        this.contributionService = Objects.requireNonNull(contributionService);
    }

    @GetMapping(path = "/{versioned_object_uid}")
    @ApiOperation(value = "Retrieves a VERSIONED_Composition associated with an EHR identified by ehr_id.", response = VersionedObjectResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSIONED_COMPOSITION is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or VERSIONED_COMPOSITION with versioned_object_uid does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<VersionedObjectResponseData<Composition>> retrieveVersionedCompositionByVersionedObjectUid(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        VersionedComposition versionedComposition = compositionService.getVersionedComposition(ehrId, versionedCompoUid);

        VersionedObjectResponseData<Composition> response = new VersionedObjectResponseData<>(versionedComposition);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/{versioned_object_uid}/revision_history")
    @ApiOperation(value = "Retrieves a VERSIONED_COMPOSITION associated with an EHR identified by ehr_id.", response = RevisionHistoryResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSIONED_COMPOSITION is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or VERSIONED_COMPOSITION with versioned_object_uid does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<RevisionHistoryResponseData> retrieveVersionedCompositionRevisionHistoryByEhr(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        RevisionHistory revisionHistory = compositionService.getRevisionHistoryOfVersionedComposition(versionedCompoUid);

        RevisionHistoryResponseData response = new RevisionHistoryResponseData(revisionHistory);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(response);
    }

    @GetMapping(path = "/{versioned_object_uid}/version/{version_uid}")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PostAuthorize("checkAbacPost(@openehrVersionedCompositionController.COMPOSITION, "
        + "@ehrService.getSubjectExtRef(#ehrIdString), returnObject, #accept)")
    @ApiOperation(value = "Retrieves a VERSION identified by version_uid of a VERSIONED_COMPOSITION identified by versioned_object_uid and associated with the EHR identified by ehr_id.", response = OriginalVersionResponseData.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Ok - requested VERSION is successfully retrieved.",
            responseHeaders = {
                @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
            }),
        @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or a VERSIONED_COMPOSITION with versioned_object_uid does not exist or a VERSION with version_uid does not exist."),
        @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByVersionUid(
        @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
        @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
        @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
        @ApiParam(value = "User supplied VERSION identifier", required = true) @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        ObjectVersionId compositionVersionId = new ObjectVersionId(versionUid);
        if (!compositionVersionId.getRoot().getValue().equals(versionedObjectUid)) {
            throw new IllegalArgumentException("Composition parameters are not matching.");
        }

        // parse given version uid
        UUID versionedObjectId;
        int version;
        try {
            versionedObjectId = UUID.fromString(compositionVersionId.getRoot().getValue());
            version = Integer.parseInt(compositionVersionId.getVersionTreeId().getValue());
        } catch (Exception e) {
            throw new InvalidApiParameterException("VERSION UID parameter has wrong format: " + e.getMessage());
        }

        // -----------------

        return getOriginalVersionResponseDataResponseEntity(accept, ehrId, versionedObjectId, version);
    }

    @GetMapping(path = "/{versioned_object_uid}/version")
    // checkAbacPre /-Post attributes (type, subject, payload, content type)
    @PostAuthorize("checkAbacPost(@openehrVersionedCompositionController.COMPOSITION, "
        + "@ehrService.getSubjectExtRef(#ehrIdString), returnObject, #accept)")
    @ApiOperation(value = "Retrieves a VERSION of a VERSIONED_COMPOSITION identified by versioned_object_uid and associated with the EHR identified by ehr_id. If version_at_time is supplied, retrieves the VERSION extant at specified time, otherwise retrieves the latest VERSION.", response = OriginalVersionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - requested VERSION is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class)
                    }),
        @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or a VERSIONED_COMPOSITION with versioned_object_uid does not exist or a VERSION with version_uid does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByTime(
            @ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @ApiParam(value = "A timestamp in the ISO8601 format", hidden = true) @RequestParam(value = "version_at_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime versionAtTime) {

        UUID ehrId = getEhrUuid(ehrIdString);
        UUID versionedCompoUid = getCompositionVersionedObjectUidString(versionedObjectUid);

        // check if parameters are valid
        checkForValidEhrAndCompositionParameter(ehrId, versionedCompoUid);

        int version;
        if (versionAtTime != null) {
            version = compositionService.getVersionByTimestamp(versionedCompoUid, versionAtTime);
        } else {
            version = compositionService.getLastVersionNumber(versionedCompoUid);
        }

        return getOriginalVersionResponseDataResponseEntity(accept, ehrId, versionedCompoUid, version);
    }

    private void checkForValidEhrAndCompositionParameter(UUID ehrId, UUID versionedCompoUid) {
        // check if EHR is valid
        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        // check if Composition is valid
        if (!compositionService.exists(versionedCompoUid)) {
            throw new ObjectNotFoundException("composition", "No composition with this ID can be found.");
        }
    }

    private ResponseEntity<OriginalVersionResponseData<Composition>> getOriginalVersionResponseDataResponseEntity(String accept,
        UUID ehrId, UUID versionedObjectId, int version) {

        Optional<OriginalVersion<Composition>> compositionOriginalVersion = compositionService.getOriginalVersionComposition(versionedObjectId, version);
        UUID contributionId = compositionOriginalVersion
            .map(i -> UUID.fromString(i.getContribution().getId().getValue()))
            .orElseThrow(() -> new InvalidApiParameterException("Couldn't retrieve Composition with given parameters"));

        Optional<ContributionDto> optionalContributionDto = contributionService.getContribution(ehrId, contributionId);
        ContributionDto contributionDto = optionalContributionDto.orElseThrow(() -> new InternalServerException("Couldn't fetch contribution for existing Composition")); // shouldn't happen

        OriginalVersionResponseData<Composition> originalVersionResponseData = new OriginalVersionResponseData<>(
            compositionOriginalVersion.orElseThrow(() -> new InternalServerException("Composition exists but can't be retrieved as Original Version.")),
            contributionDto);

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(resolveContentType(accept));

        return ResponseEntity.ok().headers(respHeaders).body(originalVersionResponseData);
    }
}
