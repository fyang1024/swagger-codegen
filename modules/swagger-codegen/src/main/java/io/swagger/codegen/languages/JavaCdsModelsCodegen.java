package io.swagger.codegen.languages;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.mustache.UppercaseLambda;
import io.swagger.models.Operation;
import io.swagger.models.Tag;

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
        List<CdsCodegenOperation> cdsCodegenOperations = operations.stream().map(o->new CdsCodegenOperation(o)).collect(Collectors.toList());
        obj.put("operation", cdsCodegenOperations);
        List<String> tagNames = operations.get(0).tags.stream().map(Tag::getName).collect(Collectors.toList());
        objs.put("tags", tagNames);
        objs.put("openBracket", "{");
        objs.put("closeBracket", "}");
        return objs;
    }

    public static class CdsCodegenOperation extends CodegenOperation {
        public boolean hasCdsScopes;
        public Object cdsScopes;
        public Set<Map.Entry<String, Object>> cdsExtensionSet;

        public CdsCodegenOperation(CodegenOperation co) {

            // Copy all fields of CodegenOperation
            this.responseHeaders.addAll(co.responseHeaders);
            this.hasAuthMethods = co.hasAuthMethods;
            this.hasConsumes = co.hasConsumes;
            this.hasProduces = co.hasProduces;
            this.hasParams = co.hasParams;
            this.hasOptionalParams = co.hasOptionalParams;
            this.returnTypeIsPrimitive = co.returnTypeIsPrimitive;
            this.returnSimpleType = co.returnSimpleType;
            this.subresourceOperation = co.subresourceOperation;
            this.isMapContainer = co.isMapContainer;
            this.isListContainer = co.isListContainer;
            this.isMultipart = co.isMultipart;
            this.hasMore = co.hasMore;
            this.isResponseBinary = co.isResponseBinary;
            this.hasReference = co.hasReference;
            this.isRestfulIndex = co.isRestfulIndex;
            this.isRestfulShow = co.isRestfulShow;
            this.isRestfulCreate = co.isRestfulCreate;
            this.isRestfulUpdate = co.isRestfulUpdate;
            this.isRestfulDestroy = co.isRestfulDestroy;
            this.isRestful = co.isRestful;
            this.path = co.path;
            this.operationId = co.operationId;
            this.returnType = co.returnType;
            this.httpMethod = co.httpMethod;
            this.returnBaseType = co.returnBaseType;
            this.returnContainer = co.returnContainer;
            this.summary = co.summary;
            this.unescapedNotes = co.unescapedNotes;
            this.notes = co.notes;
            this.baseName = co.baseName;
            this.defaultResponse = co.defaultResponse;
            this.discriminator = co.discriminator;
            this.consumes = co.consumes;
            this.produces = co.produces;
            this.bodyParam = co.bodyParam;
            this.allParams = co.allParams;
            this.bodyParams = co.bodyParams;
            this.pathParams = co.pathParams;
            this.queryParams = co.queryParams;
            this.headerParams = co.headerParams;
            this.formParams = co.formParams;
            this.authMethods = co.authMethods;
            this.tags = co.tags;
            this.responses = co.responses;
            this.imports = co.imports;
            this.examples = co.examples;
            this.externalDocs = co.externalDocs;

            // cds specific properties
            this.cdsScopes = co.vendorExtensions.get("x-scopes");
            this.hasCdsScopes = cdsScopes != null && !((List)cdsScopes).isEmpty();
            co.vendorExtensions.remove("x-accepts");
            this.cdsExtensionSet = co.vendorExtensions.entrySet();
        }
    }
}