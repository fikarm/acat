package acat.algorithm;

import acat.model.Entity;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExplorerTask extends Task<TreeItem<Entity>> {

    private Path path;
    private List<Entity> javaFiles;

    public ExplorerTask(Path path) {
        this.path = path;
    }

    @Override
    protected TreeItem<Entity> call() {

        listJavaFiles();

        assignColor();

        TreeItem<Entity> tree = groupByPackage();

        updateMessage("scan finished.");

        return tree;

    }

    private void listJavaFiles() {

        try (Stream<Path> walk = Files.walk(path)) {

            updateMessage("scanning java files");

            javaFiles = walk
                   .filter(f -> f.toString().endsWith(".java"))
                   .filter(f -> !f.toString().contains("test"))
                   .map(f -> {
                       updateMessage(f.getFileName().toString());
                       return new Entity(f);
                   })
                   .collect(Collectors.toList());

        } catch (IOException e) {

            updateMessage(e.toString());

        }

    }

    private void assignColor() {

        Map<String, Color> pkgColors = new HashMap<>();
        javaFiles.forEach(entity ->
                pkgColors.putIfAbsent(entity.packageName,Color.BLACK)
        );

        Random rand = new Random(0);
        int step =  360 / pkgColors.size();

        // Mapping Path -> Color
        Iterator<String> pkgIter = pkgColors.keySet().iterator();
        for(int i = 0, j = 0; i < 360; i += step, j++) {
            if (pkgIter.hasNext())
                pkgColors.put(pkgIter.next(), Color.hsb(i,
                        (90 + rand.nextDouble() * 10) / 100,
                        (90 + rand.nextDouble() * 10) / 100));
            else
                break;
        }

        // assign color to entities
        javaFiles.forEach( entity -> entity.color = pkgColors.get(entity.packageName) );

    }

    private TreeItem<Entity> groupByPackage() {

        updateMessage("grouping files by package name");

        TreeItem<Entity> root = new TreeItem<>(new Entity(path));

        javaFiles.forEach(entity -> {

            updateMessage(entity.name);

            TreeItem<Entity> parent = root;

            for (String pkg : entity.packageName.split("\\.")) {

                TreeItem<Entity> child = new TreeItem<>(new Entity(Paths.get(pkg)));

                // check if child exists
                boolean childExist = false;

                for (TreeItem<Entity> children : parent.getChildren()) {

                    if (children.getValue().name.equals(pkg)){

                        parent   = children;
                        childExist = true;
                        break;

                    }

                }

                if (!childExist) {

                    parent.getChildren().add(child);
                    parent = child;

                }

            }

            // add file as leaf
            parent.getChildren().add(new TreeItem<>(entity));

        });

        // just to make root dir doesn't merged with package if there is just one package
        TreeItem<Entity> filler = new TreeItem<>(new Entity(Paths.get("empty")));
        root.getChildren().add(filler);
        flatten(root);
        root.getChildren().remove(filler);

        return root;
    }

    private void flatten(TreeItem<Entity> ortu) {

        ObservableList<TreeItem<Entity>> anak;
        int                              jumlahAnak;

        anak       = ortu.getChildren();
        jumlahAnak = anak.size();

        ortu.setExpanded(true);

        updateMessage(ortu.getValue().name);

        switch (jumlahAnak){
            case  0: break;
            case  1:
                TreeItem<Entity>                 anakTunggal;
                ObservableList<TreeItem<Entity>> cucu;
                String                           namaAnakTunggal,
                                                 namaOrtu;

                anakTunggal     = anak.get(0);
                cucu            = anakTunggal.getChildren();
                namaAnakTunggal = anakTunggal.getValue().name;
                namaOrtu        = ortu.getValue().name;

                if (!anakTunggal.getValue().isFile) {
                    // gabungkan ortu dengan anak
                    ortu.getValue().setPath(Paths.get(namaOrtu + "." + namaAnakTunggal));

                    // pindahkan cucu ke ortu
                    ortu.getChildren().clear();
                    ortu.getChildren().addAll(cucu);

                    flatten(ortu);
                }
                break;
            default:
                anak.forEach(this::flatten);
                break;
        }
    }

    public List<Entity> getJavaFiles() {
        return javaFiles;
    }

}
