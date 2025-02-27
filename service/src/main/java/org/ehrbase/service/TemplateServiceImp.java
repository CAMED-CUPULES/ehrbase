/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.service;

import org.apache.xmlbeans.XmlOptions;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.ehr.knowledge.TemplateMetaData;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.jooq.DSLContext;
import org.openehr.schemas.v1.CARCHETYPEROOT;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TemplateServiceImp extends BaseServiceImp implements TemplateService {

    private final KnowledgeCacheService knowledgeCacheService;
    private final CompositionService compositionService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public TemplateServiceImp(KnowledgeCacheService knowledgeCacheService, DSLContext context, ServerConfig serverConfig, CompositionService compositionService) {
        super(knowledgeCacheService, context, serverConfig);
        this.knowledgeCacheService = Objects.requireNonNull(knowledgeCacheService);
        this.compositionService = compositionService;
    }


    @Override
    public List<TemplateMetaDataDto> getAllTemplates() {


        return knowledgeCacheService.listAllOperationalTemplates().stream().map(this::mapToDto).collect(Collectors.toList());

    }

    private TemplateMetaDataDto mapToDto(TemplateMetaData data) {
        TemplateMetaDataDto dto = new TemplateMetaDataDto();
        dto.setCreatedOn(data.getCreatedOn());

        Optional<OPERATIONALTEMPLATE> operationaltemplate = Optional.ofNullable(data.getOperationaltemplate());
        dto.setTemplateId(operationaltemplate.map(OPERATIONALTEMPLATE::getTemplateId).map(OBJECTID::getValue).orElse(null));
        dto.setArchetypeId(operationaltemplate.map(OPERATIONALTEMPLATE::getDefinition).map(CARCHETYPEROOT::getArchetypeId).map(OBJECTID::getValue).orElse(null));

        dto.setConcept(operationaltemplate.map(OPERATIONALTEMPLATE::getConcept).orElse(null));
        return dto;
    }

    @Override
    public StructuredString buildExample(String templateId, CompositionFormat format) {

        final String exampleString = "";
        //@TODO

        return new StructuredString(exampleString, StructuredStringFormat.fromCompositionFormat(format));
    }

    @Override
    public WebTemplate findTemplate(String templateId) {

        WebTemplate webTemplate;
        try {
            Optional<OPERATIONALTEMPLATE> operationaltemplate = this.knowledgeCacheService.retrieveOperationalTemplate(templateId);


            webTemplate = this.knowledgeCacheService.getQueryOptMetaData(templateId);

        } catch (NullPointerException e) {
            throw new ObjectNotFoundException("template", "Template with the specified id does not exist", e);
        } catch (Exception e) {
            throw new InternalServerException("Could not generate web template, reason:" + e);
        }

        return webTemplate;
    }

    @Override
    public String findOperationalTemplate(String templateId, OperationalTemplateFormat format) throws ObjectNotFoundException, InvalidApiParameterException, InternalServerException {
        Optional<OPERATIONALTEMPLATE> operationaltemplate;
        if (format.equals(OperationalTemplateFormat.XML)) {
            try {
                operationaltemplate = this.knowledgeCacheService.retrieveOperationalTemplate(templateId);

                if (!operationaltemplate.isPresent()) {
                    throw new ObjectNotFoundException("template", "Template with the specified id does not exist");
                }
            } catch (NullPointerException e) {      // TODO: is this NPE really thrown in case of not found template anymore?
                throw new ObjectNotFoundException("template", "Template with the specified id does not exist", e);
            }
        } else { // TODO only XML at the moment
            throw new InvalidApiParameterException("Requested operational template type not supported");
        }
        XmlOptions opts = new XmlOptions();
        opts.setSaveSyntheticDocumentElement(new QName("http://schemas.openehr.org/v1", "template"));
        return operationaltemplate.map(o -> o.xmlText(opts)).orElseThrow(() -> new InternalServerException("Failure while retrieving operational template"));
    }

    @Override
    public String create(String content) {
        return this.knowledgeCacheService.addOperationalTemplate(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean adminDeleteTemplate(String templateId) {

        Optional<OPERATIONALTEMPLATE> opt = this.knowledgeCacheService
                .retrieveOperationalTemplate(templateId);

        if (opt.isEmpty()) {
            throw new ObjectNotFoundException("ADMIN TEMPLATE", String.format(
                    "Operational template with id %s not found.", templateId
            ));
        }

        // Delete template if not used
        return this.knowledgeCacheService.deleteOperationalTemplate(opt.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String adminUpdateTemplate(String templateId, String content) {

        Optional<OPERATIONALTEMPLATE> existingTemplate =
                this.knowledgeCacheService.retrieveOperationalTemplate(templateId);

        // Check if template exists
        if (existingTemplate.isEmpty()) {
            throw new ObjectNotFoundException(
                    "ADMIN TEMPLATE UPDATE",
                    String.format("Template with id %s does not exist", templateId)
            );
        }

        // Replace content
        return this.knowledgeCacheService.adminUpdateOperationalTemplate(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adminDeleteAllTemplates() {
        return this.knowledgeCacheService.deleteAllOperationalTemplates();
    }
}
