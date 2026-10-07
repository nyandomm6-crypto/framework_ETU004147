package huhu.controler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import huhu.annotation.Json;
import huhu.utils.MethodMapp;
import huhu.view.ModelAndView;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {
    private Map<MethodMapp, Method> methods = new HashMap<>();
    private String prefix;
    private String suffix;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        Object mapping = context.getAttribute("mapping");
        methods = mapping instanceof Map
                ? (Map<MethodMapp, Method>) mapping
                : new HashMap<>();
        prefix = ServletUtil.prefix(context.getInitParameter("prefix"));
        suffix = context.getInitParameter("suffix");
        if (suffix == null) suffix = ".jsp";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = request.getRequestURI()
                .substring(request.getContextPath().length());
        if (!url.startsWith("/")) url = "/" + url;
        if (url.length() > 1 && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String httpMethod = request.getMethod().toUpperCase();
        Method method = methods.get(new MethodMapp(url, httpMethod));
        if (method == null) {
            ServletUtil.routeNotFound(response, url, httpMethod, methods);
            return;
        }

        try {
            Object controller = method.getDeclaringClass()
                    .getDeclaredConstructor().newInstance();
            Object result = method.invoke(controller,
                    ServletUtil.arguments(method, request, response));

            if (method.isAnnotationPresent(Json.class)) {
                ServletUtil.json(response, result);
            } else if (result instanceof ModelAndView view) {
                ServletUtil.view(getServletContext(), request, response, view,
                        prefix, suffix);
            } else {
                ServletUtil.text(response, result);
            }
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
