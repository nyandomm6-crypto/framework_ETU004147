package huhu.view;

public class JsonModel {

    private String code;
    private Object object;

    public JsonModel() {
    }

    public JsonModel(String code, Object object) {
        this.code = code;
        this.object = object;
    }

    public String getCode() {
        return code;
    }

    public Object getObject() {
        return object;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}