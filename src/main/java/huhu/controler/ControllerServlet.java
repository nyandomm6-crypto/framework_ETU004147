package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import huhu.utils.Utilitaire;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {

    private Map<String, Method> listMethodes = new HashMap<>();

    @Override
    public void init() throws ServletException {

        String packageName = getServletConfig().getInitParameter("controller-package");

        Utilitaire util = new Utilitaire();

        try {
            listMethodes = util.getMapping(
                    packageName,
                    huhu.annotation.Controller.class);
        } catch (Exception e) {
            throw new ServletException(e);
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

        String url = request.getRequestURI();
        String context = request.getContextPath();
        String mapping = url.substring(context.length());

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        Method methode = listMethodes.get(mapping);

        if (methode != null) {

            out.println("URL trouvée : " + mapping);
            out.println("Classe : " + methode.getDeclaringClass().getName());
            out.println("Méthode : " + methode.getName());

            // Pour invoquer la méthode plus tard :
            // Object controller = methode.getDeclaringClass()
            // .getDeclaredConstructor()
            // .newInstance();
            // Object resultat = methode.invoke(controller);

        } else {

            out.println("URL non trouvée : " + mapping);
            out.println();
            out.println("URLs disponibles :");

            for (Map.Entry<String, Method> entry : listMethodes.entrySet()) {
                out.println(entry.getKey()
                        + " -> "
                        + entry.getValue().getDeclaringClass().getSimpleName()
                        + "."
                        + entry.getValue().getName());
            }
        }
    }
}