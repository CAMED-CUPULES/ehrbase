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

package org.ehrbase.service;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rmobjectvalidator.RMObjectValidationMessage;
import com.nedap.archie.rmobjectvalidator.RMObjectValidator;
import java.util.List;
import java.util.regex.Pattern;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.terminology.openehr.TerminologyService;
import org.ehrbase.validation.CompositionValidator;
import org.ehrbase.validation.ConstraintViolationException;
import org.ehrbase.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.validation.terminology.ItemStructureVisitor;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * {@link ValidationService} implementation.
 *
 * @author Christian Chevalley
 * @since 1.0.0
 */
@Service
public class ValidationServiceImp implements ValidationService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9-_:/&+?]*");

  private static final RMObjectValidator RM_OBJECT_VALIDATOR = new RMObjectValidator(
      ArchieRMInfoLookup.getInstance(), s -> null);

  private final KnowledgeCacheService knowledgeCacheService;

  private final TerminologyService terminologyService;

  private final CompositionValidator compositionValidator = new CompositionValidator();

  public ValidationServiceImp(KnowledgeCacheService knowledgeCacheService,
      TerminologyService terminologyService, ServerConfig serverConfig,
      ObjectProvider<ExternalTerminologyValidation> objectProvider) {
    this.knowledgeCacheService = knowledgeCacheService;
    this.terminologyService = terminologyService;

    objectProvider.ifAvailable(compositionValidator::setExternalTerminologyValidation);

    if (serverConfig.isDisableStrictValidation()) {
      logger.warn("Disabling strict invariant validation. Caution is advised.");
      compositionValidator.setRunInvariantChecks(false);
    }
  }

  @Override
  public void check(String templateID, Composition composition) throws Exception {
    WebTemplate webTemplate;
    try {
      webTemplate = knowledgeCacheService.getQueryOptMetaData(templateID);
    } catch (IllegalArgumentException e) {
      throw new UnprocessableEntityException(e.getMessage());
    }

    // Validate the composition based on WebTemplate
    var constraintViolations = compositionValidator.validate(composition, webTemplate);
    if (!constraintViolations.isEmpty()) {
      throw new ConstraintViolationException(constraintViolations);
    }

    //check codephrases against terminologies
    ItemStructureVisitor itemStructureVisitor = new ItemStructureVisitor(terminologyService);
    itemStructureVisitor.validate(composition);
  }

  @Override
  public void check(Composition composition) throws Exception {
    //check if this composition is valid for processing
    if (composition.getName() == null) {
      throw new IllegalArgumentException("Composition missing mandatory attribute: name");
    }
    if (composition.getArchetypeNodeId() == null) {
      throw new IllegalArgumentException(
          "Composition missing mandatory attribute: archetype_node_id");
    }
    if (composition.getLanguage() == null) {
      throw new IllegalArgumentException("Composition missing mandatory attribute: language");
    }
    if (composition.getCategory() == null) {
      throw new IllegalArgumentException("Composition missing mandatory attribute: category");
    }
    if (composition.getComposer() == null) {
      throw new IllegalArgumentException("Composition missing mandatory attribute: composer");
    }
    if (composition.getArchetypeDetails() == null) {
      throw new IllegalArgumentException(
          "Composition missing mandatory attribute: archetype details");
    }
    if (composition.getArchetypeDetails().getTemplateId() == null) {
      throw new IllegalArgumentException(
          "Composition missing mandatory attribute: archetype details/template_id");
    }

    check(composition.getArchetypeDetails().getTemplateId().getValue(), composition);

    logger.debug("Composition validated successfully");
  }

  @Override
  public void check(EhrStatus ehrStatus) {
    //case of a system generated ehr
    if (ehrStatus == null) {
      return;
    }

    //first, check the built EhrStatus using the general Archie RM-Validator
    List<RMObjectValidationMessage> rmObjectValidationMessages = RM_OBJECT_VALIDATOR.validate(
        ehrStatus);

    if (!rmObjectValidationMessages.isEmpty()) {
      StringBuilder stringBuilder = new StringBuilder();
      for (RMObjectValidationMessage rmObjectValidationMessage : rmObjectValidationMessages) {
        stringBuilder.append(rmObjectValidationMessage.toString());
        stringBuilder.append("\n");
      }
      throw new ValidationException(stringBuilder.toString());
    }

    //second, additional specific checks and other mandatory attributes

    if (ehrStatus.getSubject() == null) {
      throw new ValidationException("subject is required");
    }

    if (ehrStatus.getSubject().getExternalRef()
        != null) {  // external_ref has 0..1 multiplicity, so null itself is okay
      // but if it is there it has to have an ID
      if (ehrStatus.getSubject().getExternalRef().getId() == null || ehrStatus.getSubject()
          .getExternalRef().getId().getValue().isEmpty()) {
        throw new ValidationException("ExternalRef ID is required");
      }
      // and a namespace
      if (ehrStatus.getSubject().getExternalRef().getNamespace() == null) {
        throw new ValidationException("ExternalRef namespace is required");
        // which needs to be valid
      } else if (!NAMESPACE_PATTERN.matcher(ehrStatus.getSubject().getExternalRef().getNamespace())
          .matches()) {
        throw new ValidationException("Subject's namespace format invalid");
      }
    }
  }
}
