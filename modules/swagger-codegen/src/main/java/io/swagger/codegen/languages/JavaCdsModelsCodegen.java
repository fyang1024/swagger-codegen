package io.swagger.codegen.languages;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.models.Operation;

import java.util.List;
import java.util.Map;

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
            co.baseName = null;
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

        // LOGGER.warn("Data type of property is: " + property.datatype);

        // if(property.baseName.equals("data")) {
        // model.isInline = true;
        // }

        // LOGGER.warn(model.name + " interface " + model.hasInterfaces);
        // LOGGER.warn("Property container type: " + property.containerType);
//        if(model.hasInterfaces) {
//
//            property.isInherited = true;
//        }

        if (property.datatype.equals("Meta") || property.datatype.equals("Links")) {
            property.isInherited = true;
        } else if (property.datatype.equals("MetaPaginated") || property.datatype.equals("LinksPaginated")) {
            property.isInherited = true;
        }
    }

    @Override
    protected String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        return super.getOrGenerateOperationId(operation, path, httpMethod.toUpperCase());
    }
}