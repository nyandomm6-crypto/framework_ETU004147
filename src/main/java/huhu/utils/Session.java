package huhu.utils;

import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

public class Session extends AbstractMap<String, Object> {
    private final HttpSession httpSession;
    private final Map<String, Object> values = new HashMap<>();

    public Session(HttpSession httpSession) {
        this.httpSession = httpSession;
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            values.put(name, httpSession.getAttribute(name));
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    public Object get(String key) {
        return values.get(key);
    }

    public void set(String key, Object value) {
        values.put(Objects.requireNonNull(key, "key"), value);
    }

    public Object remove(String key) {
        return values.remove(key);
    }

    public void save() {
        List<String> names = new ArrayList<>();
        Enumeration<String> attributes = httpSession.getAttributeNames();
        while (attributes.hasMoreElements()) {
            names.add(attributes.nextElement());
        }
        for (String name : names) {
            if (!values.containsKey(name)) {
                httpSession.removeAttribute(name);
            }
        }
        values.forEach(httpSession::setAttribute);
    }
}
