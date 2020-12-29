package acat.model;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import javafx.scene.paint.Color;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Entity {

    public boolean      isFile;
    public Path         path;
    public String       name, initial, packageName;
    public Color        color;

    public Set<String>  rp;
    public List<Double> sc, cc, ac;
    public double       ace;


    public Entity(Path path) {

        this.setPath(path);
        this.color = Color.BLACK;
        this.rp = null;
        this.sc = new ArrayList<>();
        this.cc = new ArrayList<>();
        this.ac = new ArrayList<>();

    }

    public void setPath(Path path) {

        try {

            this.path        = path;
            this.name        = path.getFileName().toString();
            this.isFile      = name.endsWith(".java");
            this.initial     = "";
            this.packageName = "";

            if(isFile) {

                StaticJavaParser.getConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);
                this.packageName = StaticJavaParser.parse(path).getPackageDeclaration().get().getNameAsString();
                this.initial =  name.replaceAll("(^\\w{2}|[A-Z\\d])|.", "$1");

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public File getFile() {
        return isFile ? path.toFile() : null;
    }

    @Override
    public String toString() {
        return isFile ? name + " (" + initial + ")" : name;
    }

}
