/*
 * Copyright 2019-2022 vitasystems GmbH and Hannover Medical School.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.rest;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * This base controller implements the basic functionality for all specific controllers. This
 * includes error handling and utils.
 *
 * @author Stefan Spiska
 * @author Jake Smolka
 * @since 1.0.0
 */
public abstract class BaseController {

  // HTTP Headers

  public static final String OPENEHR_AUDIT_DETAILS = "openEHR-AUDIT_DETAILS";

  public static final String OPENEHR_VERSION = "openEHR-VERSION";

  public static final String PREFER = "Prefer";

  public static final String RETURN_MINIMAL = "return=minimal";

  public static final String RETURN_REPRESENTATION = "return=representation";

  // Fixed header identifiers
  public static final String CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;
  public static final String ACCEPT = HttpHeaders.ACCEPT;
  public static final String REQ_CONTENT_TYPE = "Client may request content format";
  public static final String REQ_CONTENT_TYPE_BODY = "Format of transferred body";
  public static final String REQ_ACCEPT = "Client should specify expected format";
  // response headers
  public static final String RESP_CONTENT_TYPE_DESC = "Format of response";
  // Audit
  public static final String REST_OPERATION = "RestOperation";

  public static final String LOCATION = HttpHeaders.LOCATION;
  public static final String ETAG = HttpHeaders.ETAG;
  public static final String LAST_MODIFIED = HttpHeaders.LAST_MODIFIED;

  public static final String IF_MATCH = HttpHeaders.IF_MATCH;
  public static final String IF_NONE_MATCH = HttpHeaders.IF_NONE_MATCH;
  // Configuration of swagger-ui description fields
  // request headers
  public static final String REQ_OPENEHR_VERSION = "Optional custom request header for versioning";
  public static final String REQ_OPENEHR_AUDIT = "Optional custom request header for auditing";
  public static final String REQ_PREFER =
      "May be used by clients for resource representation negotiation";
  public static final String RESP_LOCATION_DESC = "Location of resource";
  public static final String RESP_ETAG_DESC = "Entity tag for resource";
  public static final String RESP_LAST_MODIFIED_DESC = "Time of last modification of resource";
  // common response description fields
  public static final String RESP_NOT_ACCEPTABLE_DESC =
      "Not Acceptable - Service can not fulfill requested format via accept header.";
  public static final String RESP_UNSUPPORTED_MEDIA_DESC =
      "Unsupported Media Type - request's content-type not supported.";

  // constants of all API resources
  public static final String EHR = "ehr";
  public static final String EHR_STATUS = "ehrstatus";
  public static final String COMPOSITION = "composition";
  public static final String DIRECTORY = "directory";
  public static final String CONTRIBUTION = "contribution";
  public static final String QUERY = "query";
  public static final String DEFINITION = "definition";

  public Map<String, Map<String, String>> add2MetaMap(
      Map<String, Map<String, String>> metaMap, String key, String value) {
    Map<String, String> contentMap;

    if (metaMap == null) {
      metaMap = new HashMap<>();
      contentMap = new HashMap<>();
      metaMap.put("meta", contentMap);
    } else {
      contentMap = metaMap.get("meta");
    }

    contentMap.put(key, value);
    return metaMap;
  }

  protected String getBaseEnvLinkURL() {
    String baseEnvLinkURL = null;
    HttpServletRequest currentRequest =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    // lazy about determining protocol but can be done too
    baseEnvLinkURL = "http://" + currentRequest.getLocalName();
    if (currentRequest.getLocalPort() != 80) {
      baseEnvLinkURL += ":" + currentRequest.getLocalPort();
    }
    if (!StringUtils.isEmpty(currentRequest.getContextPath())) {
      baseEnvLinkURL += currentRequest.getContextPath();
    }
    return baseEnvLinkURL;
  }

  /**
   * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
   * when no UUID representation can be created. This case is equal to no matching object.
   *
   * @param ehrIdString Input String representation of the ehrId
   * @return UUID representation of the ehrId
   * @throws ObjectNotFoundException when no UUID can't be created from input
   */
  protected UUID getEhrUuid(String ehrIdString) {
    return extractUUIDFromStringWithError(
        ehrIdString, "ehr", "EHR not found, in fact, only UUID-type IDs are supported");
  }

  /**
   * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
   * when no UUID representation can be created. This case is equal to no matching object.
   *
   * @param compositionVersionedObjectUidString Input String representation
   * @return UUID representation
   * @throws ObjectNotFoundException when no UUID can't be created from input
   */
  protected UUID getCompositionVersionedObjectUidString(
      String compositionVersionedObjectUidString) {
    return extractUUIDFromStringWithError(
        compositionVersionedObjectUidString,
        "composition",
        "Composition not found, in fact, only UUID-type versionedObjectUids are supported");
  }

  /**
   * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
   * when no UUID representation can be created. This case is equal to no matching object.
   *
   * @param compositionVersionedObjectUidString Input String representation
   * @return UUID representation
   * @throws ObjectNotFoundException when no UUID can't be created from input
   */
  protected UUID getContributionVersionedObjectUidString(
      String compositionVersionedObjectUidString) {
    return extractUUIDFromStringWithError(
        compositionVersionedObjectUidString,
        "contribution",
        "Contribution not found, in fact, only UUID-type versionedObjectUids are supported");
  }

  // Internal abstraction layer helper, so calling methods above can invoke with meaningful error
  // messages depending on context.
  private UUID extractUUIDFromStringWithError(String uuidString, String type, String error) {
    UUID uuid;
    try {
      uuid = UUID.fromString(uuidString);
    } catch (IllegalArgumentException e) {
      throw new ObjectNotFoundException(type, error);
    }
    return uuid;
  }

  /**
   * Extracts the {@link CompositionFormat} from the REST request's input {@link MediaType} style
   * content type header string.
   *
   * @param contentType String representation of REST request's input {@link MediaType} style
   *                    content type header
   * @return {@link CompositionFormat} expressing the content type
   * @throws NotAcceptableException when content type is not supported or input is invalid
   */
  protected CompositionFormat extractCompositionFormat(String contentType) {
    final CompositionFormat compositionFormat;

    MediaType mediaType = resolveContentType(contentType);
    if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
      compositionFormat = CompositionFormat.XML;
    } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
      compositionFormat = CompositionFormat.JSON;
    } else {
      throw new NotAcceptableException(
          "Only compositions in XML or JSON are supported at the moment");
    }
    return compositionFormat;
  }

  /**
   * Convenience helper to encode path strings to URI-safe strings
   *
   * @param path input
   * @return URI-safe escaped string
   * @throws InternalServerException when encoding failed
   */
  public String encodePath(String path) {

    path = UriUtils.encodePath(path, "UTF-8");

    return path;
  }

  /**
   * Extracts the UUID base from a versioned UID. Or, if
   *
   * @param versionUid
   * @return
   */
  protected UUID extractVersionedObjectUidFromVersionUid(String versionUid) {
    if (!versionUid.contains("::")) {
      return UUID.fromString(versionUid);
    }
    return UUID.fromString(versionUid.substring(0, versionUid.indexOf("::")));
  }

  protected int extractVersionFromVersionUid(String versionUid) {
    if (!versionUid.contains("::")) {
      return 0; // current version
    }
    // extract the version from string of format "$UUID::$SYSTEM::$VERSION"
    // via making a substring starting at last occurrence of "::" + 2
    return Integer.parseInt(versionUid.substring(versionUid.lastIndexOf("::") + 2));
  }

  /**
   * Add attribute to the current request.
   *
   * @param attributeName
   * @param value
   */
  protected void enrichRequestAttribute(String attributeName, Object value) {
    RequestContextHolder.currentRequestAttributes()
        .setAttribute(attributeName, value, RequestAttributes.SCOPE_REQUEST);
  }

  /**
   * Resolves the Content-Type based on Accept header.
   *
   * @param acceptHeader Accept header value
   * @return Content-Type of the response
   */
  protected MediaType resolveContentType(String acceptHeader) {
    return resolveContentType(acceptHeader, MediaType.APPLICATION_JSON);
  }

  /**
   * Resolves the Content-Type based on Accept header.
   *
   * @param acceptHeader     Accept header value
   * @param defaultMediaType Default Content-Type
   * @return Content-Type of the response
   */
  protected MediaType resolveContentType(String acceptHeader, MediaType defaultMediaType) {
    List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
    if (mediaTypes.isEmpty()) {
      return defaultMediaType;
    }

    MediaType.sortBySpecificityAndQuality(mediaTypes);
    MediaType contentType =
        mediaTypes.stream()
            .filter(
                mediaType ->
                    mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                        || mediaType.isCompatibleWith(MediaType.APPLICATION_XML))
            .findFirst()
            .orElseThrow(
                () -> new InvalidApiParameterException("Wrong Content-Type header in request"));

    if (contentType.equals(MediaType.ALL)) {
      return defaultMediaType;
    }

    return contentType;
  }

  protected Optional<OffsetDateTime> getVersionAtTimeParam() {
    Map<String, String> queryParams = ServletUriComponentsBuilder.fromCurrentRequest().build()
        .getQueryParams()
        .toSingleValueMap();

    String versionAtTime = queryParams.get("version_at_time");
    if (StringUtils.isBlank(versionAtTime)) {
      return Optional.empty();
    }

    try {
      return Optional.of(
          OffsetDateTime.parse(UriUtils.decode(versionAtTime, StandardCharsets.UTF_8)));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Value '" + versionAtTime + "' is not valid for version_at_time parameter. " +
              "Value must be in the extended ISO 8601 format.", e);
    }
  }
}
