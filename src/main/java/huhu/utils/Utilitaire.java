package huhu.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
}
