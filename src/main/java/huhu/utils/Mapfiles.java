package huhu.utils;

public class Mapfiles {
    private String nom;
    private String extension;
    private byte[] file;
    public Mapfiles(String nom, String extension, byte[] file) {
        this.nom = nom;
        this.extension = extension;
        this.file = file;
    }
     public Mapfiles() {
    }
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getExtension() {
        return extension;
    }
    public void setExtension(String extension) {
        this.extension = extension;
    }
    public byte[] getFile() {
        return file;
    }
    public void setFile(byte[] file) {
        this.file = file;
    }
    
}
