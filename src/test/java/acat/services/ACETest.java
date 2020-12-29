package acat.services;

import com.github.acat2.models.Entity;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ACETest {

    @Test
    public void testCase1() throws IOException {

        List<Entity> emptyList = List.of();
        String output = "/media/share/data/kuliah_s1/Semester_10/acat2/src/test/resources";

        ACE ace = new ACE( emptyList, null, 0, output );
        ace.run();

        long outputSC = Files.size( Paths.get (output + "/2-sc.txt") );
        long outputCC = Files.size( Paths.get (output + "/3-cc.txt") );
        long outputAC = Files.size( Paths.get (output + "/4-ac.txt") );
        long outputACE = Files.size( Paths.get (output + "/5-ace.txt") );

        assertEquals(0, outputSC );
        assertEquals(0, outputCC );
        assertEquals(0, outputAC );
        assertEquals(0, outputACE );

    }

    @Test
    public void testCase2() throws IOException {

        List<Entity> emptyList = List.of(
            new Entity( "classJava1", "cj1", "", "", "", 0L, new TreeSet<>() )
        );
        String output = "/media/share/data/kuliah_s1/Semester_10/acat2/src/test/resources";

        ACE aceTask = new ACE( emptyList, null, 0, output );
        aceTask.run();
        Path outputSC = Paths.get (output + "/2-sc.txt");
        Path outputCC = Paths.get (output + "/3-cc.txt");
        Path outputAC = Paths.get (output + "/4-ac.txt");
        Path outputACE = Paths.get (output + "/5-ace.txt");

        List<Double> sc = Files.lines( outputSC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> cc = Files.lines( outputCC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> ac = Files.lines( outputAC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> ace = Files.lines( outputACE )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );

        Assert.assertTrue( sc.size() == 1 && sc.get(0) == 1.0 );
        Assert.assertTrue( cc.size() == 1 && cc.get(0) == 1.0 );
        Assert.assertTrue( ac.size() == 1 && ac.get(0) == 1.0 );
        Assert.assertTrue( ace.size() == 1 && ace.get(0).equals( Double.NaN ) );

    }

    @Test
    public void testCase4() throws IOException {

        List<Entity> emptyList = List.of(
                new Entity( "classJava1", "cj1", "", "", "", 0L, new TreeSet<String>( Set.of("a") ) ),
                new Entity( "classJava2", "cj2", "", "", "", 0L, new TreeSet<String>( Set.of("a") ) )
        );
        String output = "/media/share/data/kuliah_s1/Semester_10/acat2/src/test/resources";
        double[][] pv = new double[][] { {1}, {1} };


        ACE aceTask = new ACE( emptyList, pv, 0, output );
        aceTask.run();
        Path outputSC = Paths.get (output + "/2-sc.txt");
        Path outputCC = Paths.get (output + "/3-cc.txt");
        Path outputAC = Paths.get (output + "/4-ac.txt");
        Path outputACE = Paths.get (output + "/5-ace.txt");

        List<Double> sc = Files.lines( outputSC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> cc = Files.lines( outputCC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> ac = Files.lines( outputAC )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );
        List<Double> ace = Files.lines( outputACE )
                .map( s -> Double.parseDouble( s.split( ";\\s+" )[1] ) )
                .collect( Collectors.toList() );

        System.out.println( sc );
        System.out.println( cc );
        System.out.println( ac );
        System.out.println( ace );
//        Assert.assertTrue( sc.size() > 1 && sc.get(1) == 1.0 );
//        Assert.assertTrue( cc.size() > 1 && sc.get(1) == 1.0 );
//        Assert.assertTrue( ac.size() > 1 && sc.get(1) == 1.0 );
//        Assert.assertTrue( ace.size() > 1 && sc.get(1) == 1.0 );

    }

    /*

    entities: List<Entity>

    sc = {}
    cc = {},
    ac = {},
    ace = 0

    Test cases:
    1 entities = {empty}     sc, cc, ac, ace =
    2 entities = { javaFile1 }  sc ={ 1.0 } , cc = {1.0}, ac = {1.0}, ace = 0;
    3 jalur ini tidak bisa diuji mandiri, harus menjadi bagian dari pengujian jalur dasar 3.
    4 entities = { javaFile1, javaFile2 }  javaFile1.sc ={ 1.0, ? } , javaFile1.cc = {1.0, ?}, javaFile1.ac = {1.0, ?}, javaFile1.ace = 0;
    5 jalur ini tidak bisa diuji mandiri, harus menjadi bagian dari pengujian jalur dasar 4.

    */

}