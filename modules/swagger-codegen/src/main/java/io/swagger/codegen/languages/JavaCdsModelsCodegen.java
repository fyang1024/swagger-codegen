package io.swagger.codegen.languages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.CaseFormat;
import io.swagger.codegen.*;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaCdsModelsCodegen extends AbstractJavaCodegen {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaCdsModelsCodegen.class);

    protected String title = "CDS Models Generator";
    protected String implFolder = "src/gen/java";

    public JavaCdsModelsCodegen() {
        super();

        sourceFolder = "src/gen/java";
        apiTestTemplateFiles.clear(); // TODO: add test template
        embeddedTemplateDir = templateDir = "JavaCdsModels";
        artifactId = "cds-models";

        // clear model and api doc templates
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");

        modelTemplateFiles.clear();
        apiTemplateFiles.clear();
        modelTemplateFiles.put("model.mustache", ".java");
        apiTemplateFiles.put("api.mustache", ".java");

        // clear lots of far more complicated options we don't need
        cliOptions.clear();
        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption("pathLevel", "From what depth to export controllers"));

        apiPackage = System.getProperty("swagger.codegen.cdsmodels.apipackage", "au.org.consumerdatastandards.api");
        modelPackage = System.getProperty("swagger.codegen.cdsmodels.modelpackage",
            "au.org.consumerdatastandards.api.models");

        additionalProperties.put("title", title);
        // java inflector uses the jackson lib
        additionalProperties.put("jackson", "true");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return artifactId;
    }

    @Override
    public String getHelp() {
        return "Generates a Consumer Data Standards Models.";
    }

    @Override
    public void processOpts() {
        super.processOpts();
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {
        String basePath = resourcePath;

        if (tag.endsWith("ApIs")) {
            tag = tag.replaceAll("ApIs", "Api");
            co.subresourceOperation = false;
        } else {
            if (tag.equals("Customer")) {
                tag = "CommonCustomerAPI";
            } else {
                tag = "Banking" + tag + "API";
            }
            co.subresourceOperation = true;
        }

        List<CodegenOperation> opList = operations.get(tag);

        if (opList == null) {
            opList = new ArrayList<CodegenOperation>();
            operations.put(tag, opList);
        }

        // LOGGER.warn("Tag is set to: " + tag);

        // check for operationId uniqueness

        String uniqueName = co.operationId;
        int counter = 0;
        for (CodegenOperation op : opList) {
            if (uniqueName.equals(op.operationId)) {
                uniqueName = co.operationId + "_" + counter;
                counter++;
            }
        }
        if (!co.operationId.equals(uniqueName)) {
            LOGGER.warn("generated unique operationId `" + uniqueName + "`");
        }
        co.operationId = uniqueName;
        co.operationIdLowerCase = uniqueName.toLowerCase();
        co.operationIdCamelCase = uniqueName;
        co.operationIdSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, uniqueName);
        co.baseName = basePath;
        opList.add(co);
    }

    public void postProcessModelProperty(CodegenModel m, CodegenProperty prop, Model model) {
        if (model instanceof ComposedModel) {
            prop.isInherited = true;
            ComposedModel myModel = (ComposedModel)model;
            for (Model innerModel : ((ComposedModel) model).getAllOf()) {
                if (innerModel.getProperties() != null) {
                    for (String oneProperty : innerModel.getProperties().keySet()) {
                        // LOGGER.warn("Processing " + oneProperty + " versus " + prop.baseName);
                        if (prop.baseName.equals(oneProperty)) {
                            prop.isInherited = false;
                            // LOGGER.warn("Found property within innermodel of " + prop.name);
                        }
                    }
                }

            }
        }
        postProcessModelProperty(m, prop);

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
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);

        // Add imports for Jackson
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            // for enum model
            if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                cm.imports.add(importMapping.get("JsonValue"));
                Map<String, String> item = new HashMap<String, String>();
                item.put("import", importMapping.get("JsonValue"));
                imports.add(item);
            }
        }

        return objs;
    }

    @Override
    protected String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        return super.getOrGenerateOperationId(operation, path, httpMethod.toUpperCase());
    }

    public String apiFilename(String templateName, String tag) {
        String result = super.apiFilename(templateName, tag);

        if (templateName.endsWith("api.mustache")) {
            int ix = result.indexOf(sourceFolder);
            String beg = result.substring(0, ix);
            String end = result.substring(ix + sourceFolder.length());
            new java.io.File(beg + implFolder).mkdirs();
            result = beg + implFolder + end;
        }
        return result;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        Swagger swagger = (Swagger) objs.get("swagger");
        if (swagger != null) {
            try {
                objs.put("swagger-yaml", Yaml.mapper().writeValueAsString(swagger));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultController";
        }
        return name;
        // name = name.replaceAll("[^a-zA-Z0-9]+", "_");
        // return camelize(name)+ "Controller";
    }
}