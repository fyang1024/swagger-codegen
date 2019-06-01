package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.codegen.mustache.UppercaseLambda;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class JavaCdsModelsCodegen extends AbstractJavaCodegen {

    private Swagger swagger = null;
    private Map<String, String> refParameters = new HashMap<>();
    private Set<String> refModels = new HashSet<>();

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
        return parts[0] + sanitizeName(subGroupName).replace("_", "");
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);
        this.swagger = swagger;
        preprocessParameters(swagger);
        preprocessModels(swagger);
        preprocessPaths(swagger);
    }

    private void preprocessPaths(Swagger swagger) {
        for (Path path : swagger.getPaths().values()) {
            preprocessPath(path);
        }
    }

    private void preprocessPath(Path path) {
        for(Operation operation : path.getOperations()){
            preprocessOperation(operation);
        }
    }

    private void preprocessOperation(Operation operation) {
        for (Parameter parameter : operation.getParameters()) {
            if (parameter instanceof SerializableParameter) {
                SerializableParameter sp = (SerializableParameter) parameter;
                if (sp.getEnum() != null) {
                    String referenceName = refParameters.get(sp.getName());
                    if (referenceName != null) {
                        ModelImpl enumModel = new ModelImpl().type(StringProperty.TYPE)._enum(sp.getEnum());
                        enumModel.setDescription(sp.getDescription());
                        swagger.getDefinitions().put(referenceName, enumModel);
                    }
                }
            }
            if (parameter instanceof BodyParameter) {
                Model schema = ((BodyParameter) parameter).getSchema();
                if (schema instanceof RefModel) {
                    refModels.add(((RefModel)schema).getSimpleRef());
                }
            }
        }
        for (Response response : operation.getResponses().values()) {
            if (response.getResponseSchema() instanceof RefModel) {
                refModels.add(((RefModel) response.getResponseSchema()).getSimpleRef());
            }
        }
    }

    private void preprocessModels(Swagger swagger) {
        for (Map.Entry<String, Model> entry : swagger.getDefinitions().entrySet()) {
            if (entry.getValue() instanceof ComposedModel) {
                ComposedModel composedModel = (ComposedModel) entry.getValue();
                preprocessComposedModel(composedModel);
            }
            preprocessProperties(entry.getKey(), entry.getValue());
        }
    }

    private void preprocessComposedModel(ComposedModel composedModel) {
        for (RefModel refModel : composedModel.getInterfaces()) {
            refModels.add(refModel.getSimpleRef());
        }
        Model parent = composedModel.getParent();
        if (parent instanceof RefModel) {
            refModels.add(((RefModel) parent).getSimpleRef());
        }
        Model child = composedModel.getChild();
        if (child instanceof RefModel) {
            refModels.add(((RefModel) child).getSimpleRef());
        }
    }

    private void preprocessProperties(String modelKey, Model model) {
        if (model.getProperties() != null) {
            for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
                if (entry.getValue() instanceof RefProperty) {
                    refModels.add(((RefProperty) entry.getValue()).getSimpleRef());
                } else if (entry.getValue() instanceof ArrayProperty) {
                    ArrayProperty ap = (ArrayProperty)entry.getValue();
                    if (ap.getItems() instanceof RefProperty) {
                        refModels.add(((RefProperty) ap.getItems()).getSimpleRef());
                    }
                } else if (entry.getValue() instanceof StringProperty) {
                    StringProperty sp = (StringProperty)entry.getValue();
                    if (sp.getEnum() != null && !sp.getEnum().isEmpty()) {
                        System.out.println(modelKey + "." + entry.getKey());
                        System.out.println(sp.getEnum());
                    }
                }
            }
        }
    }

    private void preprocessParameters(Swagger swagger) {
        for(Map.Entry<String, Parameter> entry : swagger.getParameters().entrySet()) {
            Parameter parameter = entry.getValue();
            String paramName = entry.getKey();
            refParameters.put(parameter.getName(), paramName);
            refModels.add(paramName);
            if (parameter instanceof BodyParameter) {
                BodyParameter bodyParameter = (BodyParameter) parameter;
                System.out.println(paramName + " -> " + bodyParameter.getSchema());
            }
        }
    }

    @Override
    public String toApiName(String name) {
        return name + "API";
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        CdsCodegenProperty cdsCodegenProperty = new CdsCodegenProperty(property);
        if (property.items != null) {
            cdsCodegenProperty.items = new CdsCodegenProperty(property.items);
        }
        replaceProperty(model, cdsCodegenProperty);
        if (cdsCodegenProperty.datatype.equals("Meta") || cdsCodegenProperty.datatype.equals("Links")) {
            cdsCodegenProperty.isInherited = true;
        } else if (cdsCodegenProperty.datatype.equals("MetaPaginated") || cdsCodegenProperty.datatype.equals("LinksPaginated")) {
            cdsCodegenProperty.isInherited = true;
        }
        if (!cdsCodegenProperty.defaultValue.equals("null") &&
            !StringUtils.isBlank(cdsCodegenProperty.defaultValue) &&
            !cdsCodegenProperty.defaultValue.startsWith("new ")) {
            cdsCodegenProperty.isDefaultValueVisible = true;
        }
        if (model.interfaces != null && !model.interfaces.isEmpty()) {
            if (cdsCodegenProperty.isEnum) {
                findEnumType(cdsCodegenProperty, model.interfaces);
            } else if (cdsCodegenProperty.items != null && cdsCodegenProperty.items.isEnum) {
                findEnumType(cdsCodegenProperty.items, model.interfaces);
            }
        }
    }

    private String findEnumType(CodegenProperty cp, List<String> interfaces) {
        for (String modelKey : interfaces) {
            Model model = swagger.getDefinitions().get(modelKey);
            if (model != null) {
                List<String> values = (List<String>)cp.allowableValues.get("values");
                String enumType = findEnumType(cp.datatypeWithEnum, values, modelKey, model);
                if (enumType != null) {
                    cp.datatype = enumType;
                    cp.datatypeWithEnum = enumType;
                    ((CdsCodegenProperty)cp).isEnumTypeExternal = true;
                    return enumType;
                }
            }
        }
        return null;
    }

    private String findEnumType(String datatypeWithEnum, List<String> values, String modelKey, Model model) {
        for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
            if (entry.getValue() instanceof StringProperty) {
                StringProperty sp = (StringProperty) entry.getValue();
                if (sp.getEnum() != null && isTheSame(values, sp.getEnum())) {
                    return modelKey + "." + datatypeWithEnum;
                }
            }
        }
        return null;
    }

    private boolean isTheSame(List<String> values1, List<String> values2) {
        if (values1.size() != values2.size()) {
            return false;
        }
        List<String> copy1 = new ArrayList<>(values1);
        Collections.sort(copy1);
        List<String> copy2 = new ArrayList<>(values2);
        Collections.sort(copy2);
        for (int i = 0; i < copy1.size(); i++) {
            if (!copy1.get(i).equals(copy2.get(i))) return false;
        }
        return true;
    }

    private void replaceProperty(CodegenModel model, CodegenProperty property) {
        replaceProperty(model.vars, property);
        replaceProperty(model.requiredVars, property);
        replaceProperty(model.optionalVars, property);
    }

    private void replaceProperty(List<CodegenProperty> properties, CodegenProperty property) {
        for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).baseName.equals(property.baseName)) {
                properties.set(i, property);
            }
        }
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CdsCodegenModel codegenModel = new CdsCodegenModel(super.fromModel(name, model, allDefinitions));
        if (model.getProperties() != null) {
            Property property = model.getProperties().get("links");
            if (property != null) {
                RefProperty refProperty = (RefProperty)property;
                if (refProperty.getSimpleRef().equals("Links")) {
                    codegenModel.isBaseResponse = true;
                } else if (refProperty.getSimpleRef().equals("LinksPaginated")) {
                    codegenModel.isPaginatedResponse = true;
                }
            }
        }
        if (model instanceof ComposedModel) {
            Model child = ((ComposedModel) model).getChild();
            codegenModel.vendorExtensions.putAll(child.getVendorExtensions());
        }
        for (CodegenProperty cp : codegenModel.vars) {
            if (cp.isEnum && !((CdsCodegenProperty)cp).isEnumTypeExternal) {
                codegenModel._enums.add(cp);
            } else if (cp.items != null && cp.items.isEnum && !((CdsCodegenProperty)cp.items).isEnumTypeExternal) {
                codegenModel._enums.add(cp.items);
            }
        }
        return codegenModel;
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
        objs.put("section", getGroupTag(operations.get(0)));
        objs.put("openBracket", "{");
        objs.put("closeBracket", "}");
        return objs;
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        super.postProcessModels(objs);
        objs.put("openBracket", "{");
        objs.put("closeBracket", "}");
        return objs;
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return sanitizeName(camelize(property.name));
    }

    private String buildCdsTypeAnnotation(String cdsType) {
        return "@CDSDataType(CustomDataType." + cdsType.replace("String", "") + ")";
    }

    private class CdsCodegenOperation extends CodegenOperation {
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
            this.vendorExtensions = co.vendorExtensions;
            this.authMethods = co.authMethods;
            this.tags = co.tags;
            this.responses = co.responses;
            this.imports = co.imports;
            this.examples = co.examples;
            this.externalDocs = co.externalDocs;

            // set cds specific properties
            this.cdsScopes = co.vendorExtensions.get("x-scopes");
            this.hasCdsScopes = cdsScopes != null && !((List)cdsScopes).isEmpty();
            this.vendorExtensions.remove("x-accepts");
        }

        public Set<Map.Entry<String, Object>> getCdsExtensionSet() {
            return vendorExtensions.entrySet();
        }
    }

    private class CdsCodegenParameter extends CodegenParameter {
        public String cdsTypeAnnotation;
        public boolean isCdsType;
        public boolean isReference;
        public String referenceName;

        CdsCodegenParameter(CodegenParameter cp) {

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
            this.allowableValues = cp.allowableValues;
            this.dataType = cp.dataType;
            this.paramName = cp.paramName;
            this.hasMore = cp.hasMore;

            // set cds specific properties
            this.isReference = refParameters.containsKey(this.baseName);
            this.referenceName = refParameters.get(this.baseName);
            if (cp.vendorExtensions != null) {
                String cdsType = (String)cp.vendorExtensions.get("x-cds-type");
                if (!StringUtils.isBlank(cdsType)) {
                    this.cdsTypeAnnotation = buildCdsTypeAnnotation(cdsType);
                    this.isCdsType = true;
                }
            }
        }

    }

    private class CdsCodegenModel extends CodegenModel {

        public boolean isSimple;
        public boolean isReferenced;
        public boolean isBaseResponse;
        public boolean isPaginatedResponse;
        public List<CodegenProperty> _enums = new ArrayList<>();

        public CdsCodegenModel(CodegenModel cm) {

            // Copy relevant fields of CodegenModel
            this.name = cm.name;
            this.classname = cm.classname;
            this.interfaces = cm.interfaces;
            this.description = cm.description;
            this.classVarName = cm.classVarName;
            this.dataType = cm.dataType;
            this.classFilename = cm.classFilename;
            this.unescapedDescription = cm.unescapedDescription;
            this.defaultValue = cm.defaultValue;
            this.arrayModelType = cm.arrayModelType;
            this.vars = cm.vars;
            this.requiredVars = cm.requiredVars;
            this.optionalVars = cm.optionalVars;
            this.allowableValues = cm.allowableValues;
            this.mandatory = cm.mandatory;
            this.allMandatory = cm.allMandatory;
            this.imports = cm.imports;
            this.hasEnums = cm.hasEnums;
            this.isEnum = cm.isEnum;
            this.isArrayModel = cm.isArrayModel;
            this.hasChildren = cm.hasChildren;
            this.externalDocs = cm.externalDocs;
            this.vendorExtensions = cm.vendorExtensions;

            // set cds specific properties
            this.isReferenced = refModels.contains(this.classname);
            this.isSimple = (this.interfaces == null || this.interfaces.isEmpty())
                && StringUtils.isBlank(this.description) && this.isReferenced;
        }

        public Set<Map.Entry<String, Object>> getCdsExtensionSet() {
            return vendorExtensions.entrySet();
        }
    }

    private class CdsCodegenProperty extends CodegenProperty {
        public String cdsTypeAnnotation;
        public boolean isCdsType;
        public boolean isDefaultValueVisible;
        public boolean isEnumTypeExternal;

        public CdsCodegenProperty(CodegenProperty cp) {

            // copy relevant fields of CodegenProperty
            this.description = cp.description;
            this.datatype = cp.datatype;
            this.datatypeWithEnum = cp.datatypeWithEnum;
            this.isContainer = cp.isContainer;
            this.required = cp.required;
            this.baseName = cp.baseName;
            this.defaultValue = cp.defaultValue;
            this.isEnum = cp.isEnum;
            this.enumName = cp.enumName;
            this.items = cp.items;
            this.allowableValues = cp.allowableValues;
            this.vendorExtensions = cp.vendorExtensions;

            // set cds specific properties
            if (cp.vendorExtensions != null) {
                String cdsType = (String)cp.vendorExtensions.get("x-cds-type");
                if (!StringUtils.isBlank(cdsType)) {
                    this.cdsTypeAnnotation = buildCdsTypeAnnotation(cdsType);
                    this.isCdsType = true;
                }
            }
        }
    }
}