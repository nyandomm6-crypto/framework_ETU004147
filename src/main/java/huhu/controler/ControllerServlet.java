package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import huhu.annotation.Json;
import huhu.utils.MethodMapp;
import huhu.view.ModelAndView;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {

    private Map<MethodMapp, Method> listMethodes = new HashMap<>();

    private String prefixe;
    private String suffixe;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {

        ServletContext context = getServletContext();

        Object mapping = context.getAttribute("mapping");

        prefixe = context.getInitParameter("prefix");
        suffixe = context.getInitParameter("suffix");

        if (prefixe == null || prefixe.isBlank()) {
            prefixe = "/WEB-INF/views/";
        } else if (!prefixe.startsWith("/")) {
            prefixe = "/" + prefixe;
        }
        if (!prefixe.endsWith("/")) {
            prefixe += "/";
        }
        if (suffixe == null) {
            suffixe = ".jsp";
        }
        if (mapping instanceof Map) {
            listMethodes = (Map<MethodMapp, Method>) mapping;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String url = request.getRequestURI()
                .substring(request.getContextPath().length());
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        if (url.length() > 1 && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String httpMethod = request.getMethod().toUpperCase();

        MethodMapp key = new MethodMapp(url, httpMethod);

        Method methode = listMethodes.get(key);

        if (methode == null) {
            writeRouteNotFound(response, url, httpMethod);
            return;
        }

        try {
            Object controller = methode.getDeclaringClass()
                    .getDeclaredConstructor()
                    .newInstance();

            Object retour = methode.invoke(controller, resolveArguments(methode, request, response));

            if (methode.isAnnotationPresent(Json.class)) {

                writeJsonResponse(response, retour);

            } else if (retour instanceof ModelAndView) {

                renderView(request, response, (ModelAndView) retour);

            } else {

                writeTextResponse(response, retour);

            }

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Object[] resolveArguments(Method methode,
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException {

        Parameter[] parameters = methode.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            Class<?> type = parameters[index].getType();

            if (type == HttpServletRequest.class) {
                arguments[index] = request;
            } else if (type == HttpServletResponse.class) {
                arguments[index] = response;
            } else {
                String name = parameters[index].getName();
                String value = request.getParameter(name);
                arguments[index] = convertParameter(name, value, type);
            }
        }

        return arguments;
    }

    private Object convertParameter(String name,
            String value,
            Class<?> type) throws ServletException {

        if (value == null) {
            if (type.isPrimitive()) {
                throw new ServletException("Paramètre obligatoire absent : " + name);
            }
            return null;
        }

        try {
            if (type == String.class) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(value);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.valueOf(value);
            }
        } catch (NumberFormatException exception) {
            throw new ServletException("Valeur invalide pour le paramètre " + name, exception);
        }

        throw new ServletException("Type de paramètre non supporté : " + type.getName());
    }

    private void writeRouteNotFound(HttpServletResponse response,
            String url,
            String httpMethod) throws IOException {

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain;charset=UTF-8");

        PrintWriter out = response.getWriter();
        out.println("Route introuvable");
        out.println("-----------------");
        out.println("URL : " + url);
        out.println("Méthode HTTP : " + httpMethod);
        out.println();
        out.println("Routes enregistrées :");

        for (Map.Entry<MethodMapp, Method> entry : listMethodes.entrySet()) {
            out.println(entry.getKey() + " -> "
                    + entry.getValue().getDeclaringClass().getSimpleName()
                    + "." + entry.getValue().getName());
        }
    }

    // private void writeJsonResponse(HttpServletResponse response,
    // Object retour) throws IOException {

    // response.setStatus(HttpServletResponse.SC_OK);
    // response.setContentType("application/json;charset=UTF-8");
    // response.getWriter().print(retour == null ? "null" : retour.toString());
    // }
    private void writeJsonResponse(HttpServletResponse response,
            Object retour) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(retour);

        PrintWriter out = response.getWriter();
        out.print(json);
    }

    private void writeTextResponse(HttpServletResponse response,
            Object retour) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(retour == null ? "" : retour);
    }

    private void renderView(HttpServletRequest request,
            HttpServletResponse response,
            ModelAndView modelAndView) throws ServletException, IOException {

        if (modelAndView.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : modelAndView.getAttributes().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        String jsp = prefixe + modelAndView.getView() + suffixe;
        System.out.println("Forward vers : " + jsp);

        if (getServletContext().getResource(jsp) == null) {
            throw new ServletException(
                    "La vue JSP '" + jsp + "' est introuvable.\n"
                            + "Vérifiez que le fichier existe dans : src/main/webapp" + jsp);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        forwardToJsp(request, response, jsp);
    }

    private void forwardToJsp(HttpServletRequest request,
            HttpServletResponse response,
            String jsp) throws ServletException, IOException {

        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("jsp");
        if (dispatcher == null) {
            throw new ServletException("Le servlet JSP de Tomcat est introuvable");
        }

        HttpServletRequest jspRequest = new HttpServletRequestWrapper(request) {
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

        dispatcher.forward(jspRequest, response);
    }
}