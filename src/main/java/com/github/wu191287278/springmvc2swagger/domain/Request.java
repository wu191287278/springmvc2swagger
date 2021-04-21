package com.github.wu191287278.springmvc2swagger.domain;

import java.util.*;

import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * @author yu.wu
 */
public class Request {

    private String clazzSimpleName;

    private String methodName;

    private String parentPath;

    private String clazzDescription;

    private String clazzNotes;

    private String summary;

    private String methodNotes;

    private String methodErrorDescription;

    private String returnDescription;

    private Property returnType;

    private Map<String, String> paramsDescription = new HashMap<>();

    private List<String> paths = new ArrayList<>();

    private List<String> headers = new ArrayList<>();

    private List<String> methods = new ArrayList<>();

    private List<String> produces = new ArrayList<>();

    private List<String> consumes = new ArrayList<>();

    private List<Parameter> parameters = new ArrayList<>();

    private Map<Integer, Response> responseStatus = new TreeMap<>();

    private boolean deprecated = false;

    public String getClazzSimpleName() {
        return clazzSimpleName;
    }

    public void setClazzSimpleName(String clazzSimpleName) {
        this.clazzSimpleName = clazzSimpleName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getClazzDescription() {
        return clazzDescription;
    }

    public void setClazzDescription(String clazzDescription) {
        this.clazzDescription = clazzDescription;
    }

    public String getClazzNotes() {
        return clazzNotes;
    }

    public void setClazzNotes(String clazzNotes) {
        this.clazzNotes = clazzNotes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMethodNotes() {
        return methodNotes;
    }

    public void setMethodNotes(String methodNotes) {
        this.methodNotes = methodNotes;
    }

    public String getMethodErrorDescription() {
        return methodErrorDescription;
    }

    public void setMethodErrorDescription(String methodErrorDescription) {
        this.methodErrorDescription = methodErrorDescription;
    }

    public String getReturnDescription() {
        return returnDescription;
    }

    public void setReturnDescription(String returnDescription) {
        this.returnDescription = returnDescription;
    }

    public Property getReturnType() {
        return returnType;
    }

    public void setReturnType(Property returnType) {
        this.returnType = returnType;
    }

    public Map<String, String> getParamsDescription() {
        return paramsDescription;
    }

    public void setParamsDescription(Map<String, String> paramsDescription) {
        this.paramsDescription = paramsDescription;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Map<Integer, Response> getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Map<Integer, Response> responseStatus) {
        this.responseStatus = responseStatus;
    }
}
