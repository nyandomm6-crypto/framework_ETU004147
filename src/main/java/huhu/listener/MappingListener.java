package huhu.listener;

import java.lang.reflect.Method;
import java.util.Map;

import huhu.annotation.Controller;
import huhu.utils.MethodMapp;
import huhu.utils.Utilitaire;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class MappingListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            ServletContext context = event.getServletContext();

            String packageName = context.getInitParameter("controller-package");
            if (packageName == null) {
                packageName = "main.java.controller"; // Valeur par défaut
            }

            Utilitaire util = new Utilitaire();
            Map<MethodMapp, Method> mapping = util.getMappingMethod(
                    packageName,
                    Controller.class);

            context.setAttribute("mapping", mapping);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur d'initialisation du mapping", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}