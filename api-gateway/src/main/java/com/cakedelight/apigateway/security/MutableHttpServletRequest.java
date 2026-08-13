package com.cakedelight.apigateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> overrides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        overrides.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String override = overrides.get(name);
        return override != null ? override : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String override = overrides.get(name);
        if (override != null) {
            return Collections.enumeration(List.of(override));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(overrides.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        return Collections.enumeration(names);
    }
}
