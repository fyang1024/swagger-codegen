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

        if (tag.equals("Customer")) {
            tag = "CommonCustomerAPI";
        } else {
            tag = "Banking" + tag + "API";
        }
        co.subresourceOperation = true;

        super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        co.baseName = resourcePath;
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