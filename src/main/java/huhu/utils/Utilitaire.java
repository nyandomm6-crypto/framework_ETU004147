package huhu.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utilitaire {
    public List<Class<?>> recupererClassesAnnotees(
            String nomPackage,
            Class<? extends Annotation> annotation)
            throws Exception {

        List<Class<?>> classesAnnotees = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String cheminPackage = nomPackage.replace('.', '/');
        URL urlPackage = classLoader.getResource(cheminPackage);

        if (urlPackage == null) {
            throw new Exception("Package introuvable : " + nomPackage);
        }

        File dossierPackage = new File(urlPackage.toURI());
        File[] fichiers = dossierPackage.listFiles();

        if (fichiers == null) {
            return classesAnnotees;
        }

        for (File fichier : fichiers) {

            if (!fichier.isFile() || !fichier.getName().endsWith(".class")) {
                continue;
            }

            String nomClasse = nomPackage + "."
                    + fichier.getName().replace(".class", "");

            Class<?> classe = Class.forName(nomClasse);

            if (classe.isAnnotationPresent(annotation)) {
                classesAnnotees.add(classe);
            }
        }

        return classesAnnotees;
    }

    public Map<Class<?>, List<Method>> getClassWithMethode(String nomPackage,
            Class<? extends Annotation> annotation, Class<? extends Annotation> annotationMet) throws Exception {

        Map<Class<?>, List<Method>> resultat = new HashMap<>();
        List<Class<?>> allClass = this.recupererClassesAnnotees(
                nomPackage,
                annotation);
        for (Class<?> classe : allClass) {
            List<Method> methodes = new ArrayList<>();
            for (Method methode : classe.getMethods()) {
                if (methode.isAnnotationPresent(annotationMet)) {
                    methodes.add(methode);
                }
            }
            resultat.put(classe, methodes);
        }

        return resultat;
    }
}
