package acat.service.nlp;

import acat.model.Entity;
import de.saxsys.javafx.test.JfxRunner;
import de.saxsys.javafx.test.TestInJfxThread;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@RunWith(JfxRunner.class)
public class PreprocessTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void mencobaTesUntukMenjalankanTask() throws InterruptedException, ExecutionException, TimeoutException {

        // initialize the task
        Preprocess task = new Preprocess(
                entities,
                "",
                false,
                false,
                false,
                false,
                0,
                0);

        // initialize the future
        CompletableFuture<String> future = new CompletableFuture<>();

        // finish the future
        task.setOnSucceeded(event -> future.complete("sukses"));
        task.setOnFailed(event -> future.complete("gagal"));

        // start the task
        task.run();

        // get future result
        String statusTask = future.get(60, TimeUnit.SECONDS );
        System.out.println( "---> " + statusTask );

        // test

    }

    @Test
    public void emptyOutputdir() {

        try {

            String outputdir = "";
            Preprocess task = new Preprocess(
                    entities,
                    outputdir,
                    false,
                    false,
                    false,
                    false,
                    0,
                    0);
            task.call();
            fail();

        } catch (IOException e) {

            System.out.println("---");
            System.out.println(e.toString());
            System.out.println("---");

            Assert.assertTrue(true);

        }

    }

    @Test
    public void nullOutputdir() {

        try {

            String outputdir = null;
            Preprocess task = new Preprocess(
                    entities,
                    outputdir,
                    false,
                    false,
                    false,
                    false,
                    0,
                    0);
            task.call();
            fail();

        } catch (IOException | NullPointerException e) {

            System.out.println("---");
            System.out.println(e.toString());
            System.out.println("---");

            Assert.assertTrue(true);

        }

    }

    // prepare
    List<Entity> entities;
    String folder = "/media/share/data/kuliah_s1/Semester_8/skripsi/leap3/samples/commons-dbutils-1.3-src/";
    Path path = Paths.get(folder);

    public PreprocessTest() {

        populateEntities();

    }

    void populateEntities() {

        entities = new ArrayList<>();
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/BasicRowProcessor.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/BeanProcessor.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/DbUtils.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/AbstractKeyedHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/AbstractListHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/ArrayHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/ArrayListHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/BeanHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/BeanListHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/ColumnListHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/KeyedHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/MapHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/MapListHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/handlers/ScalarHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/ProxyFactory.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/QueryLoader.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/QueryRunner.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/ResultSetHandler.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/ResultSetIterator.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/RowProcessor.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/wrappers/SqlNullCheckedResultSet.java" ) ) );
        entities.add( new Entity( Paths.get( folder +  "src/java/org/apache/commons/dbutils/wrappers/StringTrimmedResultSet.java" ) ) );

    }

}