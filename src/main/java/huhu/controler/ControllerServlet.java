package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import huhu.utils.Utilitaire;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {
    Map<Class<?>, List<Method>> listMethodes = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void init() throws ServletException {

        String packageName = getServletConfig().getInitParameter("controller-package");

        Utilitaire util = new Utilitaire();

        try {

            // List<Class<?>> controllers = util.recupererClassesAnnotees(
            // packageName,
            // huhu.annotation.Controller.class);

            // for (Class<?> c : controllers) {
            // listController.add(c.getName());
            // }
            listMethodes = util.getClassWithMethode(packageName, huhu.annotation.Controller.class);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuffer url = request.getRequestURL();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println(url);
        out.println("Classe controller");
        for (Map.Entry<Class<?>, List<Method>> entry : listMethodes.entrySet()) {
            out.println("Classe: " + entry.getKey().getName());
            for (Method method : entry.getValue()) {
                out.println("  Méthode: " + method.getName());
            }
            out.println("  Méthode: ");
        }
    }
}
