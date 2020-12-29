package acat.services.nlp;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class Doc2VecTest {

    @Test
    public void testPvOutputIsSameWithRepl(){

        File input = new File( "./src/test/resources/corpus.txt" );
        String outputdir = Paths.get( input.getAbsolutePath() ).getParent().toString();

        Doc2Vec pv = new Doc2Vec( input.getAbsolutePath(),
                        outputdir,
                100,
                5,
                100,
                0.05f,
                1e-5f,
                0,
                true
                );
        pv.TrainModel();

    }

}