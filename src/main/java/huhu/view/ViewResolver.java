package huhu.view;

public class ViewResolver {
    private String prefixe;
    private String suffixe;

    public ViewResolver(String prefixe, String suffixe) {
        this.prefixe = prefixe;
        this.suffixe = suffixe;
    }

    public String getPrefixe() {
        return prefixe;
    }

    public String getSuffixe() {
        return suffixe;
    }

}
