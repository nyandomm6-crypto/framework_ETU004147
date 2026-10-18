package huhu.utils;

import java.util.Objects;

import huhu.annotation.UrlMapMeth;

public class MethodMapp {

    private String url;
    private String method;

    public MethodMapp(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public MethodMapp(UrlMapMeth urlMapMeth) {
        this.url = urlMapMeth.value();
        this.method = urlMapMeth.method();
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        MethodMapp other = (MethodMapp) obj;

        return Objects.equals(url, other.url)
                && Objects.equals(method, other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

    @Override
    public String toString() {
        return "MethodMapp[url=" + url + ", method=" + method + "]";
    }
}