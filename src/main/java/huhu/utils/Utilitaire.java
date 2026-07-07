package huhu.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import huhu.annotation.UrlMapMeth;

public class Utilitaire {

    public List<Class<?>> recupererClassesAnnotees(
            String nomPackage,
            Class<? extends Annotation> annotation)
            throws Exception {

        List<Class<?>> classesAnnotees = new ArrayList<>();

        String cheminPackage = nomPackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = classLoader.getResources(cheminPackage);

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();

            if (protocol.equals("file")) {
                File dossierPackage = new File(url.toURI());
                scannerDossier(dossierPackage, nomPackage, annotation, classesAnnotees);
            } else if (protocol.equals("jar")) {
                // Cas d'un fichier JAR (déploiement)
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                scannerJar(jarPath, nomPackage, annotation, classesAnnotees);
            }
        }

        return classesAnnotees;
    }

    private void scannerDossier(
            File dossier,
            String nomPackage,
            Class<? extends Annotation> annotation,
            List<Class<?>> classesAnnotees) throws Exception {

        File[] fichiers = dossier.listFiles();
        if (fichiers == null)
            return;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                // Scanner les sous-dossiers
                scannerDossier(fichier, nomPackage + "." + fichier.getName(),
                        annotation, classesAnnotees);
            } else if (fichier.getName().endsWith(".class")) {
                String nomClasse = nomPackage + "." +
                        fichier.getName().replace(".class", "");
                try {
                    Class<?> classe = Class.forName(nomClasse);
                    if (classe.isAnnotationPresent(annotation)) {
                        classesAnnotees.add(classe);
                        System.out.println("Classe trouvée: " + nomClasse);
                    }
                } catch (ClassNotFoundException e) {

                }
            }
        }
    }

    private void scannerJar(
            String jarPath,
            String nomPackage,
            Class<? extends Annotation> annotation,
            List<Class<?>> classesAnnotees) throws IOException {

        // Décoder le chemin du JAR
        jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries(); // <-- Correction ici

            String packagePath = nomPackage.replace('.', '/');

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) &&
                        entryName.endsWith(".class") &&
                        !entry.isDirectory()) {

                    String className = entryName.replace('/', '.')
                            .replace(".class", "");

                    try {
                        Class<?> classe = Class.forName(className);
                        if (classe.isAnnotationPresent(annotation)) {
                            classesAnnotees.add(classe);
                            System.out.println(" Classe trouvée dans JAR: " + className);
                        }
                    } catch (ClassNotFoundException e) {
                        // Ignorer
                    }
                }
            }
        }
    }

    public Map<MethodMapp, Method> getMappingMethod(
            String nomPackage,
            Class<? extends Annotation> annotation) throws Exception {

        Map<MethodMapp, Method> mapping = new HashMap<>();

        System.out.println("Scan du package: " + nomPackage);
        List<Class<?>> classes = recupererClassesAnnotees(nomPackage, annotation);

        for (Class<?> classe : classes) {
            System.out.println("   Classe: " + classe.getName());
            for (Method methode : classe.getMethods()) {

                if (methode.isAnnotationPresent(UrlMapMeth.class)) {
                    System.out.println("      Méthode annotée: " + methode.getName());

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

        System.out.println("Total routes: " + mapping.size());
        return mapping;
    }

    // public List<ModelAndView> getModelAndViewList(String Suffixe, String Prefixe) {

    //     return new ArrayList<>();
    // }

}