package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import huhu.annotation.Auth;
import huhu.utils.Mapfiles;
import huhu.utils.MethodMapp;
import huhu.utils.Session;
import huhu.view.ModelAndView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

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
        Session session = null;
        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (type == HttpServletRequest.class)
                result[i] = request;
            else if (type == HttpServletResponse.class)
                result[i] = response;
            else if (type == Session.class) {
                if (session == null)
                    session = new Session(request.getSession());
                result[i] = session;
            }
            else {
                String name = parameters[i].getName();
                result[i] = parameter(name, request.getParameter(name), type, request);
            }
        }
        return result;
    }

    static boolean isAuthorized(Method method, HttpServletRequest request)
            throws ServletException {
        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        for (String name : Collections.list(session.getAttributeNames())) {
            Object principal = session.getAttribute(name);
            if (principal != null && auth.value().isInstance(principal)) {
                Object role = property(principal, auth.attributeRole());
                String roleValue = role instanceof Enum<?> enumRole
                        ? enumRole.name()
                        : role == null ? null : role.toString();
                if (!auth.attributeRoleValue().equals(roleValue)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static Object property(Object object, String property)
            throws ServletException {
        if (property.isEmpty()) {
            throw new ServletException("Le nom de la propriété de rôle est vide");
        }
        String suffix = Character.toUpperCase(property.charAt(0))
                + property.substring(1);
        for (String getterName : new String[] { "get" + suffix, "is" + suffix }) {
            try {
                Method getter = object.getClass().getMethod(getterName);
                return getter.invoke(object);
            } catch (NoSuchMethodException ignored) {
                // Try the next getter name, then fall back to a field.
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new ServletException("Impossible de lire la propriété "
                        + property + " sur " + object.getClass().getName(), exception);
            }
        }

        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(property);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                // Continue looking in the superclass.
            } catch (IllegalAccessException | RuntimeException exception) {
                throw new ServletException("Impossible de lire la propriété "
                        + property + " sur " + object.getClass().getName(), exception);
            }
        }
        throw new ServletException("Propriété de rôle introuvable : "
                + object.getClass().getName() + "." + property);
    }

    static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Authentification requise");
    }

    static void saveSession(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof Session session) {
                session.save();
                return;
            }
        }
    }

    private static Object parameter(String name, String value, Class<?> type,
            HttpServletRequest request) throws ServletException {
        if (value == null) {
            Object file = file(name, type, request);
            if (file != null)
                return file;
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
                Object file = file(name, fieldType, request);
                if (file != null) {
                    converted = file;
                } else if (value == null) {
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
                    && (request.getParameter(prefix + "." + field.getName()) != null
                            || field.getType() == Mapfiles.class
                                    && hasFilePart(prefix + "." + field.getName(), request))) {
                return object(prefix, type, request);
            }
        }
        return null;
    }

    private static boolean hasFilePart(String name, HttpServletRequest request)
            throws ServletException {
        try {
            Part part = request.getPart(name);
            return part != null && part.getSubmittedFileName() != null
                && !part.getSubmittedFileName().isEmpty();
        } catch (IOException exception) {
            throw new ServletException("Impossible de lire le fichier " + name,
                exception);
        }
    }

    private static Object file(String name, Class<?> type,
            HttpServletRequest request) throws ServletException {
        if (type != Mapfiles.class)
            return null;
        try {
            Part part = request.getPart(name);
            if (part == null || part.getSubmittedFileName() == null
                    || part.getSubmittedFileName().isEmpty()) {
                return null;
            }

            String fileName = Paths.get(part.getSubmittedFileName())
                    .getFileName().toString();
            Path directory = uploadDirectory(request);
            Files.createDirectories(directory);
            Files.write(directory.resolve(fileName), part.getInputStream().readAllBytes());

            Mapfiles file = new Mapfiles();
            file.setFile(Files.readAllBytes(directory.resolve(fileName)));
            file.setNom(fileName);
            int extensionIndex = fileName.lastIndexOf('.');
            file.setExtension(extensionIndex > 0 && extensionIndex < fileName.length() - 1
                    ? fileName.substring(extensionIndex + 1) : "");
            return file;
        } catch (IOException | IllegalStateException exception) {
            throw new ServletException("Impossible d'enregistrer le fichier " + name,
                    exception);
        }
    }

    private static Path uploadDirectory(HttpServletRequest request) throws ServletException {
        String location = request.getServletContext().getInitParameter("upload-path");
        if (location == null || location.isBlank()) {
            throw new ServletException("Le paramètre de contexte 'upload-path' est absent");
        }
        return Path.of(location);
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
