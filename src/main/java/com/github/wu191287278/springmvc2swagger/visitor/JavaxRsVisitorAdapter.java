package com.github.wu191287278.springmvc2swagger.visitor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.wu191287278.springmvc2swagger.domain.Request;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class JavaxRsVisitorAdapter extends VoidVisitorAdapter<Swagger> {

    private ResolveSwaggerType resolveSwaggerType = new ResolveSwaggerType();

    private final Set<String> controllers = new HashSet<>(Arrays.asList("Path"));

    private final Set<String> mappings = new HashSet<>(Arrays.asList("Path", "GET",
            "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS"));


    private final Map<String, String> methods = new HashMap<>();

    public JavaxRsVisitorAdapter() {
        methods.put("GET", "get");
        methods.put("POST", "post");
        methods.put("DELETE", "delete");
        methods.put("PUT", "put");
        methods.put("PATCH", "patch");
        methods.put("OPTIONS", "options");
        methods.put("HEAD", "head");
    }


    @Override
    public void visit(MethodDeclaration n, Swagger swagger) {
        List<AnnotationExpr> annotationExprs = n.getAnnotations()
                .stream()
                .filter(a -> mappings.contains(a.getNameAsString()))
                .collect(Collectors.toList());

        if (annotationExprs.isEmpty()) return;


        Request request = new Request();
        Map<String, Path> paths = swagger.getPaths();
        parse((ClassOrInterfaceDeclaration) n.getParentNode().get(), n, request);


        String parentMappingPath = request.getParentPath() == null ? "" : request.getParentPath();
        Operation operation = new Operation()
                .tag(request.getClazzSimpleName())
                .consumes(request.getConsumes().isEmpty() ? null : request.getConsumes())
                .produces(request.getProduces().isEmpty() ? null : request.getProduces())
                .description(request.getMethodNotes())
                .summary(request.getSummary())
                .response(200, new Response()
                        .description(request.getReturnDescription() == null ? "" : request.getReturnDescription())
                        .schema(request.getReturnType())
                );
        operation.setParameters(request.getParameters());
        if (request.getMethodErrorDescription() != null) {
            operation.response(500, new Response().description("{\"message\":\"" + request.getMethodErrorDescription() + "\"}"));
        }

        //方法上如果只打入注解没有url,将使用类上的url
        List<String> pathList = request.getPaths();
        if (pathList.isEmpty()) {
            String fullPath = ("/" + parentMappingPath).replaceAll("[/]+", "/");
            Path path = paths.computeIfAbsent(fullPath, s -> new Path());
            paths.put(fullPath, path);
            for (String method : request.getMethods()) {
                path.set(method, operation.operationId(n.getNameAsString()));
            }
            if (request.getMethods().isEmpty()) {
                path.set("get", operation.operationId(n.getNameAsString()));
            }
        }

        for (String methodPath : pathList) {
            String fullPath = ("/" + parentMappingPath + "/" + methodPath).replaceAll("[/]+", "/");
            Path path = paths.computeIfAbsent(fullPath, s -> new Path());
            paths.put(fullPath, path);
            for (String method : request.getMethods()) {
                path.set(method, operation.operationId(n.getNameAsString()));
            }
            if (request.getMethods().isEmpty()) {
                path.set("get", operation.operationId(n.getNameAsString()));
            }
        }


        super.visit(n, swagger);
    }


    @Override
    public void visit(ClassOrInterfaceDeclaration n, Swagger swagger) {
        List<AnnotationExpr> annotationExprs = n.getAnnotations()
                .stream()
                .filter(a -> controllers.contains(a.getNameAsString()))
                .collect(Collectors.toList());

        if (annotationExprs.isEmpty()) return;
        Tag tag = new Tag()
                .name(n.getNameAsString());
        swagger.addTag(tag);
        n.getJavadoc().ifPresent(c -> tag.description(StringUtils.isBlank(c.getDescription().toText()) ? null : c.getDescription().toText()));
        super.visit(n, swagger);

    }

    private void parse(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, MethodDeclaration n, Request request) {
        request.setClazzSimpleName(classOrInterfaceDeclaration.getNameAsString());
        parseMapping(classOrInterfaceDeclaration, n, request);
        parseMethodParameters(n, request);
        parseReturnType(n, request);
    }


    private void parseMapping(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, MethodDeclaration n, Request request) {
        for (AnnotationExpr annotation : classOrInterfaceDeclaration.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            if (mappings.contains(annotationName)) {
                if (annotation instanceof SingleMemberAnnotationExpr) {
                    SingleMemberAnnotationExpr singleMemberAnnotationExpr = (SingleMemberAnnotationExpr) annotation;
                    request.setParentPath(singleMemberAnnotationExpr.getMemberValue().asStringLiteralExpr().asString());
                }
            }
        }

        for (AnnotationExpr annotation : n.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            if (mappings.contains(annotationName)) {
                if (annotation instanceof SingleMemberAnnotationExpr) {
                    SingleMemberAnnotationExpr singleMemberAnnotationExpr = (SingleMemberAnnotationExpr) annotation;
                    Expression memberValue = singleMemberAnnotationExpr.getMemberValue();
                    if (memberValue.isStringLiteralExpr()) {
                        request.getPaths().add(memberValue.asStringLiteralExpr().asString());
                    }
                }
            }
        }
    }

    private void parseMethodParameters(MethodDeclaration n, Request request) {
        request.setMethodName(n.getName().asString());
        parseMethodComment(n, request);
        for (AnnotationExpr annotation : n.getAnnotations()) {
            String name = annotation.getName().asString();
            String method = methods.get(name);
            if (method != null) {
                request.getMethods().add(method);
            }
        }


        for (Parameter parameter : n.getParameters()) {
            String typeName = parameter.getType().toString();
            if (typeName.contains("HttpServletRequest") || typeName.contains("HttpServletResponse")) {
                continue;
            }
            String variableName = parameter.getNameAsString();
            Map<String, String> paramsDescription = request.getParamsDescription();
            String description = paramsDescription.get(variableName);
            Property property = resolveSwaggerType.resolve(parameter.getType());
            Property paramProperty = property instanceof ObjectProperty ? new StringProperty()
                    .description(property.getDescription()) : property;
            io.swagger.models.parameters.Parameter param = new QueryParameter()
                    .property(paramProperty);
            if (parameter.getAnnotations().isEmpty() && property instanceof ObjectProperty) {
                try {
                    ObjectProperty objectProperty = (ObjectProperty) property;
                    if (objectProperty.getProperties() != null && objectProperty.getProperties().size() > 0) {
                        for (Map.Entry<String, Property> entry : objectProperty.getProperties().entrySet()) {
                            Property value = entry.getValue();
                            QueryParameter queryParameter = new QueryParameter()
                                    .name(entry.getKey())
                                    .description(value.getDescription())
                                    .example(value.getExample() == null ? null : value.getExample().toString())
                                    .required(value.getRequired())
                                    .format(value.getFormat())
                                    .type(value.getType());
                            request.getParameters().add(queryParameter);
                        }
                        continue;
                    }
                } catch (Exception e) {
                }
            }
            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                String annotationName = annotation.getNameAsString();

                if (annotation.isAnnotationExpr()) {
                    switch (annotationName) {
                        case "Path":
                            param = new PathParameter()
                                    .property(paramProperty);
                            break;
                        case "BeanParam":
                            Model model = resolveSwaggerType.convertToModel(property);
                            BodyParameter bodyParameter = new BodyParameter().schema(new ModelImpl().type("object"));
                            if (model != null) {
                                bodyParameter.schema(model);
                            }
                            param = bodyParameter;
                            break;

                        case "FormParam":
                            param = new FormParameter()
                                    .property(paramProperty);
                            request.getConsumes().add("multipart/form-data");
                            break;
                        case "HeaderParam":
                            param = new HeaderParameter()
                                    .property(paramProperty);
                            break;
                        case "CookieParam":
                            param = new CookieParameter()
                                    .property(property);
                            break;
                        case "SpringQueryMap":
                            try {
                                if (property instanceof ObjectProperty) {
                                    ObjectProperty objectProperty = (ObjectProperty) property;
                                    if (objectProperty.getProperties() != null && objectProperty.getProperties().size() > 0) {
                                        for (Map.Entry<String, Property> entry : objectProperty.getProperties().entrySet()) {
                                            Property value = entry.getValue();
                                            QueryParameter queryParameter = new QueryParameter()
                                                    .name(entry.getKey())
                                                    .description(value.getDescription())
                                                    .example(value.getExample() == null ? null : value.getExample().toString())
                                                    .required(value.getRequired())
                                                    .format(value.getFormat())
                                                    .type(value.getType());
                                            request.getParameters().add(queryParameter);
                                        }
                                    }
                                    param = null;
                                    break;
                                }
                            } catch (Exception e) {
                            }
                    }
                }
            }

            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                String annotationName = annotation.getNameAsString();
                if ("Consumes".equals(annotationName)) {
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isStringLiteralExpr()) {
                                request.getConsumes().add(value.asStringLiteralExpr().asString());
                            }
                        }
                    }
                }
                if ("Produces".equals(annotationName)) {
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isStringLiteralExpr()) {
                                request.getProduces().add(value.asStringLiteralExpr().asString());
                            }
                        }
                    }
                }

                if ("DefaultValue".equals(annotationName)) {
                    if (annotation.isStringLiteralExpr()) {
                        property.setDefault(annotation.asStringLiteralExpr().asString());
                    }
                }


                if ("NotNull".equals(annotationName)) {
                    property.setRequired(true);
                }

                if ("NotEmpty".equals(annotationName)) {
                    property.setAllowEmptyValue(false);
                }

                if ("NotBlank".equals(annotationName)) {
                    property.setAllowEmptyValue(false);
                    property.setRequired(true);
                }

                if ("Pattern".equalsIgnoreCase(annotationName)) {
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isStringLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("regexp")) {
                                    request.getProduces().add(value.asStringLiteralExpr().asString());
                                }
                            }
                        }
                    }
                    if (annotation.isStringLiteralExpr()) {
                        if (property instanceof StringProperty) {
                            StringProperty stringProperty = (StringProperty) property;
                            String pattern = annotation.asStringLiteralExpr().asString();
                            stringProperty.pattern(pattern);
                        }
                    }

                }

                if ("Min".equalsIgnoreCase(annotationName)) {
                    if (annotation.isLongLiteralExpr()) {
                        if (property instanceof AbstractNumericProperty) {
                            AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                            long l = annotation.asLongLiteralExpr().asLong();
                            numericProperty.minimum(BigDecimal.valueOf(l));
                        }
                    }
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isLongLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("value") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.minimum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("Max".equalsIgnoreCase(annotationName)) {
                    if (annotation.isLongLiteralExpr()) {
                        if (property instanceof AbstractNumericProperty) {
                            AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                            long l = annotation.asLongLiteralExpr().asLong();
                            numericProperty.maximum(BigDecimal.valueOf(l));
                        }
                    }
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isLongLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("value") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.maximum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("Range".equalsIgnoreCase(annotationName)) {
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isLongLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("min") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.minimum(BigDecimal.valueOf(l));
                                }
                                if (name.equalsIgnoreCase("max") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.maximum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("DecimalMin".equalsIgnoreCase(annotationName)) {
                    if (annotation.isLongLiteralExpr()) {
                        if (property instanceof AbstractNumericProperty) {
                            AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                            long l = annotation.asLongLiteralExpr().asLong();
                            numericProperty.maximum(BigDecimal.valueOf(l));
                        }
                    }
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isLongLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("value") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.maximum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("DecimalMax".equalsIgnoreCase(annotationName)) {
                    if (annotation.isStringLiteralExpr()) {
                        if (property instanceof AbstractNumericProperty) {
                            AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                            String l = annotation.asStringLiteralExpr().asString();
                            numericProperty.maximum(new BigDecimal(l));
                        }
                    }
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isStringLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("value") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.maximum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("Size".equalsIgnoreCase(annotationName)) {
                    if (annotation.isLongLiteralExpr()) {
                        if (property instanceof AbstractNumericProperty) {
                            AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                            String l = annotation.asStringLiteralExpr().asString();
                            numericProperty.maximum(new BigDecimal(l));
                        }
                    }
                    if (annotation.isArrayInitializerExpr()) {
                        NodeList<Expression> values = annotation.asArrayInitializerExpr().getValues();
                        for (Expression value : values) {
                            if (value.isLongLiteralExpr()) {
                                String name = value.asNameExpr().getName().asString();
                                if (name.equalsIgnoreCase("min") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.minimum(BigDecimal.valueOf(l));
                                }
                                if (name.equalsIgnoreCase("max") && property instanceof AbstractNumericProperty) {
                                    AbstractNumericProperty numericProperty = (AbstractNumericProperty) property;
                                    long l = annotation.asLongLiteralExpr().asLong();
                                    numericProperty.maximum(BigDecimal.valueOf(l));
                                }
                            }
                        }
                    }
                }
                if ("Email".equalsIgnoreCase(annotationName)) {
                    property.setExample("api_docs@swagger.io");
                }

                if ("URL".equalsIgnoreCase(annotationName)) {
                    property.setExample("https://swagger.io");
                }

            }


            if (param != null) {
                param.setDescription(property.getDescription() != null ? property.getDescription() : description);
                param.setName(variableName);
                request.getParameters().add(param);
            }
        }
    }

    private void parseMethodComment(MethodDeclaration n, Request request) {
        n.getJavadocComment().ifPresent(c -> {
            if (c.isJavadocComment()) {
                JavadocComment javadocComment = c.asJavadocComment();
                Javadoc parse = javadocComment.parse();
                JavadocDescription description = parse.getDescription();
                request.setSummary(description.toText());
                for (JavadocBlockTag blockTag : parse.getBlockTags()) {
                    switch (blockTag.getTagName().toLowerCase()) {
                        case "throws":
                            request.setMethodErrorDescription(blockTag.getContent().toText());
                            break;
                        case "return":
                            request.setReturnDescription(blockTag.getContent().toText());
                            break;
                        case "apinote":
                            request.setMethodNotes(blockTag.getContent().toText());
                            break;
                        case "responsestatus":
                            try {
                                String text = blockTag.getContent().toText();
                                String[] split = text.trim().split("\\s+|\\t");
                                if (split.length > 1 && NumberUtils.isDigits(split[0])) {
                                    String reason = String.join("", Arrays.copyOfRange(split, 1, split.length));
                                    Response response = new Response();
                                    response.description(reason);
                                    request.getResponseStatus().put(Integer.parseInt(split[0]), response);
                                }
                            } catch (Exception e) {
                            }
                            break;
                        default:
                            blockTag.getName().ifPresent(t -> request.getParamsDescription().put(t, blockTag.getContent().toText()));
                            break;
                    }
                }
            }
        });
    }

    private void parseReturnType(MethodDeclaration n, Request request) {
        Type type = n.getType();
        if (type.isVoidType()) {
            return;
        }

        Property property = resolveSwaggerType.resolve(type);
        if (property.getName() != null) {
            request.setReturnType(new RefProperty("#/definitions/" + property.getName()));
        } else {
            request.setReturnType(property);
        }
    }

    public Map<String, Model> getModelMap() {
        return resolveSwaggerType.getModelMap();
    }

}
