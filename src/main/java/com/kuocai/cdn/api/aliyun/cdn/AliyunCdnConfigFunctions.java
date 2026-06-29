package com.kuocai.cdn.api.aliyun.cdn;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AliyunCdnConfigFunctions {
    private final List<Function> functions;

    public AliyunCdnConfigFunctions() {
        this.functions = new ArrayList<>();
    }

    public String getFunctionNames() {
        HashSet<String> functionNames = new HashSet<>();
        for (Function function : functions) {
            functionNames.add(function.getFunctionName());
        }
        return String.join(",", functionNames);
    }

    @Data
    public static class Function {
        private String functionName;
        private List<FunctionArg> functionArgs;
        private String parentId = "-1";

        public Function() {
            this.functionArgs = new ArrayList<>();
        }

        public Function(String functionName) {
            this.functionName = functionName;
            this.functionArgs = new ArrayList<>();
        }

        public void setFunctionArg(FunctionArg functionArg) {
            functionArgs.add(functionArg);
        }

        public void setFunctionArgs(FunctionArg... functionArgs) {
            this.functionArgs.addAll(Arrays.asList(functionArgs));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FunctionArg {
        private String argName;
        private String argValue;
    }

    public void setFunction(Function function) {
        functions.add(function);
    }

    public void setFunctions(Function... functions) {
        this.functions.addAll(Arrays.asList(functions));
    }

    public String getJson() {
        JSONArray data = new JSONArray();
        for (Function function : functions) {
            JSONObject functionJson = new JSONObject();
            functionJson.put("functionName", function.getFunctionName());
            JSONArray functionArgs = new JSONArray();
            for (FunctionArg functionArg : function.getFunctionArgs()) {
                JSONObject functionArgJson = new JSONObject();
                functionArgJson.put("argName", functionArg.getArgName());
                functionArgJson.put("argValue", functionArg.getArgValue());
                functionArgs.add(functionArgJson);
            }
            functionJson.put("functionArgs", functionArgs);
            functionJson.put("parentId", function.getParentId());
            data.add(functionJson);
        }
        return data.toJSONString();
    }
}
