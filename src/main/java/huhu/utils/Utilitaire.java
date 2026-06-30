package huhu.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import huhu.annotation.UrlMap;
import huhu.annotation.UrlMapMeth;

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

    public Map<String, Method> getMapping(String nomPackage,
            Class<? extends Annotation> annotation) throws Exception {

        Map<String, Method> mapping = new HashMap<>();

        List<Class<?>> classes = recupererClassesAnnotees(nomPackage, annotation);

        for (Class<?> classe : classes) {
            for (Method methode : classe.getMethods()) {

                if (methode.isAnnotationPresent(UrlMap.class)) {

                    String url = methode.getAnnotation(UrlMap.class).value();

                    if (mapping.containsKey(url)) {
                        Method ancienne = mapping.get(url);
                        throw new Exception(
                                "URL '" + url + "' déjà déclarée dans "
                                        + ancienne.getDeclaringClass().getName() + "."
                                        + ancienne.getName()
                                        + " et "
                                        + classe.getName() + "." + methode.getName());
                    }

                    mapping.put(url, methode);
                }
            }
        }

        return mapping;
    }

    public Map<MethodMapp, Method> getMappingMethod(
            String nomPackage,
            Class<? extends Annotation> annotation) throws Exception {

        Map<MethodMapp, Method> mapping = new HashMap<>();

        List<Class<?>> classes = recupererClassesAnnotees(nomPackage, annotation);

        for (Class<?> classe : classes) {
            for (Method methode : classe.getMethods()) {

                if (methode.isAnnotationPresent(UrlMapMeth.class)) {

                    MethodMapp key = new MethodMapp(
                            methode.getAnnotation(UrlMapMeth.class));

                    if (mapping.containsKey(key)) {

                        Method ancienne = mapping.get(key);

                        throw new Exception(
                                "Route '" + key.getMethod() + " " + key.getUrl()
                                        + "' déjà déclarée dans "
                                        + ancienne.getDeclaringClass().getName()
                                        + "." + ancienne.getName()
                                        + " et "
                                        + classe.getName()
                                        + "." + methode.getName());
                    }

                    mapping.put(key, methode);
                }
            }
        }

        return mapping;
    }
}
