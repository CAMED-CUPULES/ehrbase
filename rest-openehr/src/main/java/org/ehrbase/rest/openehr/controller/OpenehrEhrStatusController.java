package org.ehrbase.rest.openehr.controller;

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.openehr.response.EhrStatusResponseData;
import org.ehrbase.rest.openehr.response.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

/**
 * Controller for /ehr/{ehrId}/ehr_status resource of openEHR REST API
 */
@Api(tags = {"EHR_STATUS"})
@RestController
@RequestMapping(path = "/rest/openehr/v1/ehr/{ehr_id}/ehr_status", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrEhrStatusController extends BaseController {

    private final EhrService ehrService;

    @Autowired
    public OpenehrEhrStatusController(EhrService ehrService) {
        this.ehrService = Objects.requireNonNull(ehrService);
    }

    /*@GetMapping(params = {"version_at_time"})
    @ApiOperation(value = "Retrieves the version of the EHR_STATUS associated with the EHR identified by ehr_id. If version_at_time is supplied, retrieves the version extant at specified time, otherwise retrieves the latest EHR_STATUS version.", response = EhrStatusResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - EHR resource is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with supplied subject parameters does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format."),
            @ApiResponse(code = 415, message = "Unsupported Media Type - Type not supported.")})
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusByTime(@ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                                                   @ApiParam(value = "Timestamp", required = true) @RequestParam(value = "version_at_time") String versionAtTime) {

        Optional<EhrStatus> ehrStatus = ehrService.getEhrStatus()

        Optional<UUID> ehrIdOpt = ehrService.findBySubject(subjectId, subjectNamespace);

        UUID ehrId = ehrIdOpt.orElseThrow(() -> new ObjectNotFoundException("ehr", "No EHR with supplied subject parameters found"));

        return internalGetEhrProcessing(accept, ehrId);
    }*/

    @GetMapping(path = "/{version_uid}")
    @ApiOperation(value = "Retrieves a particular version of the EHR_STATUS identified by version_uid and associated with the EHR identified by ehr_id.", response = EhrStatusResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok - equested EHR_STATUS is successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = String.class)
                    }),
            @ApiResponse(code = 404, message = "Not Found - EHR with ehr_id does not exist or when an EHR_STATUS with version_uid does not exist."),
            @ApiResponse(code = 406, message = "Not Acceptable - Service can not fulfil requested Accept format.")})
    public ResponseEntity<EhrStatusResponseData> retrieveEhrStatusById(@ApiParam(value = "Client should specify expected response format") @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                                                       @ApiParam(value = "User supplied EHR ID", required = true) @PathVariable(value = "ehr_id") String ehrIdString,
                                                                       @ApiParam(value = "User supplied version UID of EHR_STATUS", required = true) @PathVariable(value = "version_uid") String versionUid) {

        UUID ehrId = getEhrUuid(ehrIdString);

        if(ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR with this ID can be found");
        }

        UUID versionedObjectUid = extractVersionedObjectUidFromVersionUid(versionUid);
        int version = extractVersionFromVersionUid(versionUid);

        Optional<EhrStatus> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, versionedObjectUid, version);

        UUID ehrStatusId = UUID.fromString(ehrStatus.orElseThrow(() -> new ObjectNotFoundException("ehr_status", "EHR_STATUS not found")).getUid().toString());

        return internalGetEhrStatusProcessing(accept, ehrId, ehrStatusId, version);
    }

    private ResponseEntity<EhrStatusResponseData> internalGetEhrStatusProcessing(String accept, UUID ehrId, UUID ehrStatusId, int version) {
        List<String> headerList = Arrays.asList(CONTENT_TYPE, LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec

        Optional<InternalResponse<EhrStatusResponseData>> respData = buildEhrStatusResponseData(EhrStatusResponseData::new, ehrId, ehrStatusId, version, accept, headerList);

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PutMapping
    @ApiOperation(value = "Update status of the specified EHR")
    @OperationNotesResourcesReaderOpenehr.ApiNotes("ehrStatusPut.md")
    public ResponseEntity<Void> updateEhrStatus(@ApiParam(value = "EHR ID", required = true) @PathVariable("ehr_id") UUID ehrId,
                                                @ApiParam(value = "EHR status.", required = true) @RequestBody() EhrStatus ehrStatus) {
        // FIXME EHR_STATUS: pipe this through general method with e.g. buildEhrStatusResponseData(() -> null, ehrId, ehrStatusId, version, accept, headerList)
        Optional<EhrStatus> updateStatus = ehrService.updateStatus(ehrId, ehrStatus);
        URI url = URI.create(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId.toString() + "/ehr_status/" + updateStatus.get().getUid().getValue());
        return ResponseEntity.noContent().header(HttpHeaders.LOCATION, url.toString()).build();
    }

    /**
     * Builder method to prepare appropriate HTTP response. Flexible to either allow minimal or full representation of resource.
     *
     * @param factory       Lambda function to constructor of desired object
     * @param ehrId         Ehr reference
     * @param ehrStatusId   EhrStatus versioned object ID
     * @param version       EhrStatus version number
     * @param accept        Requested content format
     * @param headerList    Requested headers that need to be set
     * @param <T>           Either only header response or specific class EhrStatusResponseData
     * @return
     */
    private <T extends EhrStatusResponseData> Optional<InternalResponse<T>> buildEhrStatusResponseData(Supplier<T> factory, UUID ehrId, UUID ehrStatusId, int version, String accept, List<String> headerList) {
        // create either EhrStatusResponseData or null (means no body, only headers incl. link to resource), via lambda request
        T minimalOrRepresentation = factory.get();

        // check for valid format header to produce content accordingly
        MediaType contentTypeHeaderInput;  // to prepare header input if this header is needed later
        if (StringUtils.isBlank(accept) || accept.equals("*/*")) {  // "*/*" is standard for "any mime-type"
            // assign default if no header was set
            contentTypeHeaderInput = MediaType.APPLICATION_JSON;
        } else {
            // if header was set process it
            MediaType mediaType = MediaType.parseMediaType(accept);

            if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                contentTypeHeaderInput = MediaType.APPLICATION_JSON;
            } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
                contentTypeHeaderInput = MediaType.APPLICATION_XML;
            } else {
                throw new InvalidApiParameterException("Wrong Content-Type header in request");
            }
        }

        if (minimalOrRepresentation != null) {
            // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
            EhrStatusResponseData objByReference = (EhrStatusResponseData) minimalOrRepresentation;

            Optional<EhrStatus> ehrStatus = ehrService.getEhrStatusAtVersion(ehrId, ehrStatusId, version);
            if (ehrStatus.isPresent()) {
                objByReference.setArchetypeNodeId(ehrStatus.get().getArchetypeNodeId());
                objByReference.setName(ehrStatus.get().getName());
                objByReference.setUid(ehrStatus.get().getUid());    // FIXME EHR_STATUS: here(?) ::scheme::version is needed too
                objByReference.setSubject(ehrStatus.get().getSubject());
                objByReference.setOtherDetails(ehrStatus.get().getOtherDetails());
                objByReference.setModifiable(ehrStatus.get().isModifiable());
                objByReference.setQueryable(ehrStatus.get().isQueryable());
            } else {
                return Optional.empty();
            }
        }

        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {
                case CONTENT_TYPE:
                    respHeaders.setContentType(contentTypeHeaderInput);
                    break;
                case LOCATION:
                    try {
                        URI url = new URI(getBaseEnvLinkURL() + "/rest/openehr/v1/ehr/" + ehrId + "/ehr_status/" + ehrStatusId + "::XYZ::" + version); // FIXME EHR_STATUS: domain ::XYZ:: problem
                        respHeaders.setLocation(url);
                    } catch (Exception e) {
                        throw new InternalServerException(e.getMessage());
                    }
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + ehrStatusId + "::XYZ::" + version + "\"");   // FIXME EHR_STATUS: domain ::XYZ:: problem
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for now
                    respHeaders.setLastModified(123124442);
                    break;
            }
        }

        return Optional.of(new InternalResponse<>(minimalOrRepresentation, respHeaders));
    }
}
