package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.mustache.UppercaseLambda;
import io.swagger.models.Operation;
import io.swagger.models.Tag;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaCdsModelsCodegen extends AbstractJavaCodegen {

    public JavaCdsModelsCodegen() {

        super();
        sourceFolder = "src/gen/java";
        embeddedTemplateDir = templateDir = "JavaCdsModels";
        artifactId = "cds-models";
        apiPackage = "au.org.consumerdatastandards.api";
        modelPackage = "au.org.consumerdatastandards.api.models";
        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTestTemplateFiles.clear();
        additionalProperties.put("uppercase", new UppercaseLambda());
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.OTHER;
    }

    @Override
    public String getName() {
        return artifactId;
    }

    @Override
    public String getHelp() {
        return "Generate Consumer Data Standards Models.";
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {

        String groupTag = getGroupTag(co);
        List<CodegenOperation> group = operations.get(groupTag);
        if (!contains(group, co)) {
            super.addOperationToGroup(groupTag, resourcePath, operation, co, operations);
        }
    }

    private boolean contains(List<CodegenOperation> group, CodegenOperation co) {
        if (group == null || group.isEmpty()) return false;
        for (CodegenOperation o : group) {
            if (o.operationId.equals(co.operationId) && o.httpMethod.equals(co.httpMethod)) {
                return true;
            }
        }
        return false;
    }

    private String getGroupTag(CodegenOperation co) {
        String groupName = co.tags.get(0).getName();
        String subGroupName = co.tags.get(1).getName();
        String[] parts = groupName.split(" ");
        return parts[0] + sanitizeName(subGroupName).replace("_", "") + parts[1];
    }

    @Override
    public String toApiName(String name) {
        return name;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if (property.datatype.equals("Meta") || property.datatype.equals("Links")) {
            property.isInherited = true;
        } else if (property.datatype.equals("MetaPaginated") || property.datatype.equals("LinksPaginated")) {
            property.isInherited = true;
        }
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        super.postProcessOperations(objs);
        Map<String, Object> obj = (Map<String, Object>)objs.get("operations");
        List<CodegenOperation> operations = (List<CodegenOperation>) obj.get("operation");
        List<CdsCodegenOperation> cdsCodegenOperations = operations.stream().map(CdsCodegenOperation::new).collect(Collectors.toList());
        obj.put("operation", cdsCodegenOperations);
        List<String> tagNames = operations.get(0).tags.stream().map(Tag::getName).collect(Collectors.toList());
        objs.put("tags", tagNames);
        objs.put("openBracket", "{");
        objs.put("closeBracket", "}");
        return objs;
    }

    private static class CdsCodegenOperation extends CodegenOperation {
        public boolean hasCdsScopes;
        public Object cdsScopes;
        public Set<Map.Entry<String, Object>> cdsExtensionSet;

        CdsCodegenOperation(CodegenOperation co) {

            // Copy relevant fields of CodegenOperation
            this.responseHeaders.addAll(co.responseHeaders);
            this.hasAuthMethods = co.hasAuthMethods;
            this.hasConsumes = co.hasConsumes;
            this.hasProduces = co.hasProduces;
            this.hasParams = co.hasParams;
            this.returnTypeIsPrimitive = co.returnTypeIsPrimitive;
            this.returnSimpleType = co.returnSimpleType;
            this.isMapContainer = co.isMapContainer;
            this.isListContainer = co.isListContainer;
            this.hasMore = co.hasMore;
            this.hasReference = co.hasReference;
            this.path = co.path;
            this.operationId = co.operationId;
            this.returnType = co.returnType;
            this.httpMethod = co.httpMethod;
            this.returnBaseType = co.returnBaseType;
            this.summary = co.summary;
            this.notes = co.notes;
            this.baseName = co.baseName;
            this.defaultResponse = co.defaultResponse;
            this.discriminator = co.discriminator;
            this.allParams = co.allParams.stream().map(CdsCodegenParameter::new).collect(Collectors.toList());
            this.authMethods = co.authMethods;
            this.tags = co.tags;
            this.responses = co.responses;
            this.imports = co.imports;
            this.examples = co.examples;
            this.externalDocs = co.externalDocs;

            // set cds specific properties
            this.cdsScopes = co.vendorExtensions.get("x-scopes");
            this.hasCdsScopes = cdsScopes != null && !((List)cdsScopes).isEmpty();
            co.vendorExtensions.remove("x-accepts");
            this.cdsExtensionSet = co.vendorExtensions.entrySet();
        }
    }

    private static class CdsCodegenParameter extends CodegenParameter {
        public String cdsTypeAnnotation;
        public boolean isCdsType;

        public CdsCodegenParameter(CodegenParameter cp) {

            // copy relevant fields of CodegenParameter
            this.baseName = cp.baseName;
            this.description = cp.description;
            this.isHeaderParam = cp.isHeaderParam;
            this.isBodyParam = cp.isBodyParam;
            this.isPathParam = cp.isPathParam;
            this.isCookieParam = cp.isCookieParam;
            this.isFormParam = cp.isFormParam;
            this.isQueryParam = cp.isQueryParam;
            this.required = cp.required;
            this.defaultValue = cp.defaultValue;
            this.isEnum = cp.isEnum;
            this.datatypeWithEnum = cp.datatypeWithEnum;
            this.dataType = cp.dataType;
            this.paramName = cp.paramName;
            this.hasMore = cp.hasMore;

            // set cds specific properties
            if (cp.vendorExtensions != null) {
                String cdsType = (String)cp.vendorExtensions.get("x-cds-type");
                if (!StringUtils.isBlank(cdsType)) {
                    this.cdsTypeAnnotation = buildCdsTypeAnnotation(cdsType);
                    this.isCdsType = true;
                }
            }
        }

        private String buildCdsTypeAnnotation(String cdsType) {
            StringBuilder sb = new StringBuilder();
            sb.append("@CDSDataType(CustomDataType.").append(cdsType.replace("String", "")).append(")");
            return sb.toString();
        }
    }
}