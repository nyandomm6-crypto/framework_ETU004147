package huhu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

import jakarta.servlet.http.HttpSession;

public class Session {
    private Map<String, Object> donnee = new HashMap<>();
    private HttpSession httpSession;

    public Session(HttpSession httpSession) {
        this.httpSession = httpSession;

        Enumeration<String> noms = httpSession.getAttributeNames();

        while (noms.hasMoreElements()) {
            String nom = noms.nextElement();
            Object valeur = httpSession.getAttribute(nom);
            donnee.put(nom, valeur);
        }
    }

    public void set(String cle, Object valeur) {
        donnee.put(cle, valeur);
        httpSession.setAttribute(cle, valeur);
    }

    public Object get(String cle) {
        return donnee.get(cle);
    }

    public void remove(String cle) {
        donnee.remove(cle);
        httpSession.removeAttribute(cle);
    }

}