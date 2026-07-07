package huhu.controler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import huhu.utils.MethodMapp;
import huhu.view.ModelAndView;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
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

        prefixe = "/WEB-INF/views/";
        suffixe = ".jsp";

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

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String url = requestUri.substring(contextPath.length());
        String httpMethod = request.getMethod();

        MethodMapp key = new MethodMapp(url, httpMethod);

        Method methode = listMethodes.get(key);

        if (methode == null) {

            response.setContentType("text/plain;charset=UTF-8");

            PrintWriter out = response.getWriter();

            out.println("Route introuvable");
            out.println("-----------------");
            out.println("URL : " + url);
            out.println("Méthode HTTP : " + httpMethod);
            out.println();

            out.println("Routes enregistrées :");

            for (Map.Entry<MethodMapp, Method> entry : listMethodes.entrySet()) {
                out.println(entry.getKey()
                        + " -> "
                        + entry.getValue().getDeclaringClass().getSimpleName()
                        + "."
                        + entry.getValue().getName());
            }

            return;
        }

        try {

            Object controller = methode.getDeclaringClass()
                    .getDeclaredConstructor()
                    .newInstance();

            Object retour = methode.invoke(controller);

            if (retour instanceof ModelAndView) {

                ModelAndView mv = (ModelAndView) retour;

                if (mv.getAttributes() != null) {
                    for (Map.Entry<String, Object> entry : mv.getAttributes().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                String jsp = prefixe + mv.getView() + suffixe;

                System.out.println("Forward vers : " + jsp);

                if (getServletContext().getResource(jsp) == null) {
                    throw new ServletException(
                            "La vue JSP '" + jsp + "' est introuvable.\n"
                                    + "Vérifiez que le fichier existe dans : src/main/webapp" + jsp);
                }

                request.getRequestDispatcher(jsp).forward(request, response);
                return;
            }

            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println(retour);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}