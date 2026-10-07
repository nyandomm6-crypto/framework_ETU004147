package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import huhu.utils.MethodMapp;
import huhu.view.ModelAndView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

final class ServletUtil {
    private ServletUtil() {
    }

    static String prefix(String value) {
        if (value == null || value.isBlank())
            value = "/WEB-INF/views/";
        else if (!value.startsWith("/"))
            value = "/" + value;
        return value.endsWith("/") ? value : value + "/";
    }

    static Object[] arguments(Method method, HttpServletRequest request,
            HttpServletResponse response) throws ServletException {
        Parameter[] parameters = method.getParameters();
        Object[] result = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (type == HttpServletRequest.class)
                result[i] = request;
            else if (type == HttpServletResponse.class)
                result[i] = response;
            else {
                String name = parameters[i].getName();
                result[i] = parameter(name, request.getParameter(name), type, request);
            }
        }
        return result;
    }

    private static Object parameter(String name, String value, Class<?> type,
            HttpServletRequest request) throws ServletException {
        if (value == null) {
            if (type.isPrimitive()) {
                throw new ServletException("Paramètre obligatoire absent : " + name);
            }
            Object object = objectIfPresent(name, type, request);
            return object == null ? null : object;
        }
        if (type == String.class)
            return value;
        if (primitive(type))
            return primitive(name, value, type);
        return object(name, type, request);
    }

    private static Object object(String prefix, Class<?> type,
            HttpServletRequest request) throws ServletException {
        try {
            Object object = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()))
                    continue;
                String name = prefix + "." + field.getName();
                Class<?> fieldType = field.getType();
             String value = request.getParameter(name);

Object converted;

if (value == null) {
    converted = objectIfPresent(name, fieldType, request);
} else {
    converted = parameter(name, value, fieldType, request);
}
                if (converted != null) {
                    field.setAccessible(true);
                    field.set(object, converted);
                }
            }
            return object;
        } catch (Exception exception) {
            throw new ServletException("Impossible de construire l'objet "
                    + type.getName(), exception);
        }
    }

    private static Object objectIfPresent(String prefix, Class<?> type,
            HttpServletRequest request) throws ServletException {
        if (type == String.class || primitive(type))
            return null;
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    && request.getParameter(prefix + "." + field.getName()) != null) {
                return object(prefix, type, request);
            }
        }
        return null;
    }

    private static Object primitive(String name, String value, Class<?> type)
            throws ServletException {
        try {
            if (type == int.class || type == Integer.class)
                return Integer.valueOf(value);
            if (type == long.class || type == Long.class)
                return Long.valueOf(value);
            if (type == double.class || type == Double.class)
                return Double.valueOf(value);
            if (type == boolean.class || type == Boolean.class)
                return Boolean.valueOf(value);
            if (type == float.class || type == Float.class)
                return Float.valueOf(value);
            if (type == short.class || type == Short.class)
                return Short.valueOf(value);
            if (type == byte.class || type == Byte.class)
                return Byte.valueOf(value);
            if ((type == char.class || type == Character.class)
                    && value.length() == 1) {
                return value.charAt(0);
            }
        } catch (NumberFormatException exception) {
            throw invalid(name, value, exception);
        }
        throw new ServletException("Valeur invalide pour le paramètre "
                + name + " : " + value);
    }

    private static ServletException invalid(String name, String value,
            Exception cause) {
        return new ServletException("Valeur invalide pour le paramètre "
                + name + " : " + value, cause);
    }

    private static boolean primitive(Class<?> type) {
        return type.isPrimitive() || type == Integer.class || type == Long.class
                || type == Double.class || type == Boolean.class || type == Float.class
                || type == Short.class || type == Byte.class || type == Character.class;
    }

    static void routeNotFound(HttpServletResponse response, String url,
            String method, Map<MethodMapp, Method> methods) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("Route introuvable\n-----------------");
        out.println("URL : " + url);
        out.println("Méthode HTTP : " + method + "\n");
        out.println("Routes enregistrées :");
        for (Map.Entry<MethodMapp, Method> entry : methods.entrySet()) {
            out.println(entry.getKey() + " -> "
                    + entry.getValue().getDeclaringClass().getSimpleName()
                    + "." + entry.getValue().getName());
        }
    }

    static void json(HttpServletResponse response, Object value) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(new ObjectMapper().writeValueAsString(value));
    }

    // static void json(
    // HttpServletResponse response,
    // Object value) throws IOException {

    // response.setStatus(HttpServletResponse.SC_OK);
    // response.setContentType("application/json");
    // response.setCharacterEncoding("UTF-8");

    // Map<String, Object> result = new HashMap<>();

    // result.put("objet", value.getClass().getSimpleName());
    // result.put("valeur", value);

    // response.getWriter().print(
    // new ObjectMapper().writeValueAsString(result)
    // );
    // }

    static void text(HttpServletResponse response, Object value) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(value == null ? "" : value);
    }

    static void view(ServletContext context, HttpServletRequest request,
            HttpServletResponse response, ModelAndView model, String prefix,
            String suffix) throws ServletException, IOException {
        if (model.getAttributes() != null) {
            model.getAttributes().forEach(request::setAttribute);
        }
        String jsp = prefix + model.getView() + suffix;
        if (context.getResource(jsp) == null) {
            throw new ServletException("La vue JSP '" + jsp + "' est introuvable.");
        }
        RequestDispatcher dispatcher = context.getNamedDispatcher("jsp");
        if (dispatcher == null) {
            throw new ServletException("Le servlet JSP de Tomcat est introuvable");
        }
        response.setStatus(HttpServletResponse.SC_OK);
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return getContextPath() + jsp;
            }

            @Override
            public String getServletPath() {
                return jsp;
            }

            @Override
            public String getPathInfo() {
                return null;
            }
        };
        dispatcher.forward(wrapped, response);
    }
}
