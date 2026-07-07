package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import huhu.utils.MethodMapp;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {

    private Map<MethodMapp, Method> listMethodes = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        // Récupérer le mapping depuis le contexte de l'application
        ServletContext context = getServletContext();
        Object mapping = context.getAttribute("mapping");

        if (mapping != null && mapping instanceof Map) {
            listMethodes = (Map<MethodMapp, Method>) mapping;
        } else {
            listMethodes = new HashMap<>();
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

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = requestUri.substring(contextPath.length());

        String httpMethod = request.getMethod();

        MethodMapp key = new MethodMapp(url, httpMethod);

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        Method methode = listMethodes.get(key);

        if (methode != null) {

            out.println("Route trouvée");
            out.println("----------------");
            out.println("URL : " + url);
            out.println("HTTP : " + httpMethod);
            out.println("Classe : " + methode.getDeclaringClass().getName());
            out.println("Méthode : " + methode.getName());

            try {
                Object controller = methode.getDeclaringClass()
                        .getDeclaredConstructor()
                        .newInstance();
                Object retour = methode.invoke(controller);
                out.println("Retour = " + retour);
            } catch (Exception e) {
                throw new ServletException(e);
            }

        } else {

            out.println("Route introuvable");
            out.println("-----------------");
            out.println("URL demandée : " + url);
            out.println("HTTP : " + httpMethod);

            out.println();
            out.println("Routes enregistrées (" + listMethodes.size() + ") :");

            for (Map.Entry<MethodMapp, Method> entry : listMethodes.entrySet()) {
                out.println(entry.getKey()
                        + " -> "
                        + entry.getValue().getDeclaringClass().getSimpleName()
                        + "."
                        + entry.getValue().getName());
            }
        }
    }
}