package acat.models;

import java.util.ArrayList;
import java.util.TreeSet;

public class Entity {

    private final FileType fileType;
    private boolean hasJavaDescendant;
    private String name;
    private String initial;
    private String packageName;
    private String sourceCode;
    private String classType;
    private long loc;
    private TreeSet<String> rp;
    private ArrayList<Double> sc;
    private ArrayList<Double> cc;
    private ArrayList<Double> ac;
    private double ace;

    // konstruktor untuk entitas dengan tipe file selain JAVA_CLASS
    public Entity (String fileName, FileType type ) {

        this.fileType = type;
        this.name = fileName;

    }

    // konstruktor untuk entitas dengan tipe file JAVA_CLASS
    public Entity (String fileName,
                   String initialName,
                   String packageName,
                   String sourceCode,
                   String classType,
                   Long linesOfCode,
                   TreeSet<String> relevantProperties ) {

        this.fileType = FileType.JAVA_CLASS;
        this.name = fileName;
        this.initial = initialName;
        this.packageName = packageName;
        this.sourceCode = sourceCode;
        this.classType = classType;
        this.loc = linesOfCode;
        this.rp = relevantProperties;
        this.sc = new ArrayList<>();
        this.cc = new ArrayList<>();
        this.ac = new ArrayList<>();
        this.ace = 0;

    }

    @Override public String toString() {
        return name;
    }

    public String getInitial() {
        return initial;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean isHasJavaDescendant() {
        return hasJavaDescendant;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public TreeSet<String> getRp() {
        return rp;
    }

    public ArrayList<Double> getSc() {
        return sc;
    }

    public ArrayList<Double> getCc() {
        return cc;
    }

    public ArrayList<Double> getAc() {
        return ac;
    }

    public double getAce() {
        return ace;
    }

    public String getClassType() {
        return classType;
    }

    public long getLoc() {
        return loc;
    }


    public void setName( String fileName ) {
        name = fileName;
    }

    public void setAce( double ace ) {
        this.ace = ace;
    }

    public void setHasJavaDescendant( boolean hasJavaDescendant ) {
        this.hasJavaDescendant = hasJavaDescendant;
    }

}
