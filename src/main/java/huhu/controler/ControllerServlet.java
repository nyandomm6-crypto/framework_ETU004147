package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import huhu.utils.Utilitaire;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {
    List<String> listController = new ArrayList<>();

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

            List<Class<?>> controllers = util.recupererClassesAnnotees(
                    packageName,
                    huhu.annotation.Controller.class);

            for (Class<?> c : controllers) {
                listController.add(c.getName());
            }

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
        for (String controller : listController) {
            out.println(controller);
        }
    }
}
