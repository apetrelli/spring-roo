package org.springframework.roo.addon.solr;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.roundtrip.XmlRoundTripFileManager;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperationsImpl;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Metadata listener responsible for installing Web MVC JSP artifacts for the
 * Solr search addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class SolrJspMetadataListener implements MetadataProvider,
        MetadataNotificationListener {

    // Fields
    @Reference private MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference private MetadataService metadataService;
    @Reference private FileManager fileManager;
    @Reference private TilesOperations tilesOperations;
    @Reference private MenuOperations menuOperations;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private PersistenceMemberLocator persistenceMemberLocator;
    @Reference private PathResolver pathResolver;
    @Reference private TypeLocationService typeLocationService;
    @Reference private XmlRoundTripFileManager xmlRoundTripFileManager;

    private WebScaffoldMetadata webScaffoldMetadata;
    private JpaActiveRecordMetadata jpaActiveRecordMetadata;
    private JavaType javaType;
    private JavaType formbackingObject;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                SolrWebSearchMetadata.getMetadataIdentiferType(),
                getProvidesType());
    }

    public MetadataItem get(final String metadataIdentificationString) {
        javaType = SolrJspMetadata.getJavaType(metadataIdentificationString);
        LogicalPath path = SolrJspMetadata
                .getPath(metadataIdentificationString);
        String solrWebSearchMetadataKeyString = SolrWebSearchMetadata
                .createIdentifier(javaType, path);
        SolrWebSearchMetadata webSearchMetadata = (SolrWebSearchMetadata) metadataService
                .get(solrWebSearchMetadataKeyString);
        if (webSearchMetadata == null || !webSearchMetadata.isValid()) {
            return null;
        }

        webScaffoldMetadata = (WebScaffoldMetadata) metadataService
                .get(WebScaffoldMetadata.createIdentifier(javaType, path));
        Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");

        formbackingObject = webScaffoldMetadata.getAnnotationValues()
                .getFormBackingObject();
        jpaActiveRecordMetadata = (JpaActiveRecordMetadata) metadataService
                .get(JpaActiveRecordMetadata.createIdentifier(
                        formbackingObject, path));
        Assert.notNull(
                jpaActiveRecordMetadata,
                "Could not determine entity metadata for type: "
                        + javaType.getFullyQualifiedTypeName());

        installMvcArtifacts(webScaffoldMetadata);

        return new SolrJspMetadata(metadataIdentificationString,
                webSearchMetadata);
    }

    public void installMvcArtifacts(
            final WebScaffoldMetadata webScaffoldMetadata) {
        copyArtifacts("form/search.tagx", "WEB-INF/tags/form/search.tagx");
        copyArtifacts("form/fields/search-facet.tagx",
                "WEB-INF/tags/form/fields/search-facet.tagx");
        copyArtifacts("form/fields/search-field.tagx",
                "WEB-INF/tags/form/fields/search-field.tagx");

        LogicalPath path = WebScaffoldMetadata.getPath(webScaffoldMetadata
                .getId());
        xmlRoundTripFileManager.writeToDiskIfNecessary(pathResolver
                .getIdentifier(
                        Path.SRC_MAIN_WEBAPP.getModulePathId(path.getModule()),
                        "WEB-INF/views/"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getPath() + "/search.jspx"),
                getSearchDocument(webScaffoldMetadata));

        String folderName = webScaffoldMetadata.getAnnotationValues().getPath();
        tilesOperations.addViewDefinition(folderName, path, folderName
                + "/search", TilesOperationsImpl.DEFAULT_TEMPLATE,
                "WEB-INF/views/"
                        + webScaffoldMetadata.getAnnotationValues().getPath()
                        + "/search.jspx");
        menuOperations.addMenuItem(
                new JavaSymbolName(formbackingObject.getSimpleTypeName()),
                new JavaSymbolName("solr"), new JavaSymbolName(
                        jpaActiveRecordMetadata.getPlural())
                        .getReadableSymbolName(), "global.menu.find", "/"
                        + webScaffoldMetadata.getAnnotationValues().getPath()
                        + "?search", "s:", path);
    }

    private Document getSearchDocument(
            final WebScaffoldMetadata webScaffoldMetadata) {
        // Next install search.jspx
        Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");

        DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        Document document = builder.newDocument();

        // Add document namespaces
        Element div = new XmlElementBuilder("div", document)
                .addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
                .addAttribute("xmlns:fields",
                        "urn:jsptagdir:/WEB-INF/tags/form/fields")
                .addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
                .addAttribute("version", "2.0")
                .addChild(
                        new XmlElementBuilder("jsp:output", document)
                                .addAttribute("omit-xml-declaration", "yes")
                                .build()).build();
        document.appendChild(div);

        Element pageSearch = new XmlElementBuilder("page:search", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("ps:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("path",
                        webScaffoldMetadata.getAnnotationValues().getPath())
                .build();
        pageSearch.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(pageSearch));

        final List<FieldMetadata> idFields = persistenceMemberLocator
                .getIdentifierFields(formbackingObject);
        if (idFields.isEmpty()) {
            return null;
        }
        Element resultTable = new XmlElementBuilder("fields:table", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("rt:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("data", "${searchResults}")
                .addAttribute("delete", "false")
                .addAttribute("update", "false")
                .addAttribute("path",
                        webScaffoldMetadata.getAnnotationValues().getPath())
                .addAttribute(
                        "typeIdFieldName",
                        formbackingObject.getSimpleTypeName().toLowerCase()
                                + "."
                                + idFields.get(0).getFieldName()
                                        .getSymbolName().toLowerCase()
                                + SolrUtils.getSolrDynamicFieldPostFix(idFields
                                        .get(0).getFieldType())).build();
        resultTable.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(resultTable));

        StringBuilder facetFields = new StringBuilder();
        int fieldCounter = 0;

        ClassOrInterfaceTypeDetails formbackingClassOrInterfaceDetails = typeLocationService
                .getTypeDetails(formbackingObject);
        Assert.notNull(formbackingClassOrInterfaceDetails,
                "Unable to obtain physical type metadata for type "
                        + formbackingObject.getFullyQualifiedTypeName());
        MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(
                getClass().getName(), formbackingClassOrInterfaceDetails);
        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(formbackingObject);
        final MethodMetadata versionAccessor = persistenceMemberLocator
                .getVersionAccessor(formbackingObject);

        for (MethodMetadata method : memberDetails.getMethods()) {
            // Only interested in accessors
            if (!BeanInfoUtils.isAccessorMethod(method)) {
                continue;
            }
            if (++fieldCounter < 7) {
                if (method.getMethodName().equals(
                        identifierAccessor.getMethodName())
                        || method.getMethodName().equals(
                                versionAccessor.getMethodName())) {
                    continue;
                }
                if (method.hasSameName(identifierAccessor, versionAccessor)) {
                    continue;
                }

                FieldMetadata field = BeanInfoUtils.getFieldForJavaBeanMethod(
                        memberDetails, method);
                if (field == null) {
                    continue;
                }

                facetFields
                        .append(formbackingObject.getSimpleTypeName()
                                .toLowerCase())
                        .append(".")
                        .append(field.getFieldName())
                        .append(SolrUtils.getSolrDynamicFieldPostFix(field
                                .getFieldType())).append(",");

                Element columnElement = new XmlElementBuilder("fields:column",
                        document)
                        .addAttribute(
                                "id",
                                XmlUtils.convertId("c:"
                                        + formbackingObject
                                                .getFullyQualifiedTypeName()
                                        + "."
                                        + field.getFieldName().getSymbolName()))
                        .addAttribute(
                                "property",
                                formbackingObject.getSimpleTypeName()
                                        .toLowerCase()
                                        + "."
                                        + field.getFieldName().getSymbolName()
                                                .toLowerCase()
                                        + SolrUtils
                                                .getSolrDynamicFieldPostFix(field
                                                        .getFieldType()))
                        .build();
                columnElement.setAttribute("z",
                        XmlRoundTripUtils.calculateUniqueKeyFor(columnElement));
                resultTable.appendChild(columnElement);
            }
        }

        Element searchFacet = new XmlElementBuilder("fields:search-facet",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("sfacet:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("facetFields", facetFields.toString()).build();
        searchFacet.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(searchFacet));
        pageSearch.appendChild(searchFacet);

        Element searchField = new XmlElementBuilder("fields:search-field",
                document).addAttribute(
                "id",
                XmlUtils.convertId("sfield:"
                        + webScaffoldMetadata.getAnnotationValues()
                                .getFormBackingObject()
                                .getFullyQualifiedTypeName())).build();
        searchField.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(searchField));

        pageSearch.appendChild(searchFacet);
        pageSearch.appendChild(searchField);
        pageSearch.appendChild(resultTable);

        div.appendChild(pageSearch);

        return document;
    }

    public String getProvidesType() {
        return SolrJspMetadata.getMetadataIdentiferType();
    }

    public void notify(final String upstreamDependency,
            String downstreamDependency) {
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            Assert.isTrue(
                    MetadataIdentificationUtils.getMetadataClass(
                            upstreamDependency).equals(
                            MetadataIdentificationUtils
                                    .getMetadataClass(SolrWebSearchMetadata
                                            .getMetadataIdentiferType())),
                    "Expected class-level notifications only for Solr web search metadata (not '"
                            + upstreamDependency + "')");

            // A physical Java type has changed, and determine what the
            // corresponding local metadata identification string would have
            // been
            JavaType javaType = SolrWebSearchMetadata
                    .getJavaType(upstreamDependency);
            LogicalPath path = SolrWebSearchMetadata
                    .getPath(upstreamDependency);
            downstreamDependency = SolrJspMetadata.createIdentifier(javaType,
                    path);

            // We only need to proceed if the downstream dependency relationship
            // is not already registered
            // (if it's already registered, the event will be delivered directly
            // later on)
            if (metadataDependencyRegistry.getDownstream(upstreamDependency)
                    .contains(downstreamDependency)) {
                return;
            }
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Assert.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '"
                        + downstreamDependency
                        + "' to this provider (which uses '"
                        + getProvidesType() + "'");

        metadataService.evict(downstreamDependency);
        if (get(downstreamDependency) != null) {
            metadataDependencyRegistry.notifyDownstream(downstreamDependency);
        }
    }

    private void copyArtifacts(final String relativeTemplateLocation,
            final String relativeProjectFileLocation) {
        // First install search.tagx
        String projectFileLocation = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, relativeProjectFileLocation);
        if (!fileManager.exists(projectFileLocation)) {
            try {
                FileCopyUtils.copy(FileUtils.getInputStream(getClass(),
                        relativeTemplateLocation),
                        fileManager.createFile(projectFileLocation)
                                .getOutputStream());
            }
            catch (IOException e) {
                throw new IllegalStateException("Could not copy "
                        + relativeProjectFileLocation + " into project", e);
            }
        }
    }
}
