package acat.services;

import javafx.beans.property.SimpleIntegerProperty;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static java.lang.Math.*;
import static org.junit.Assert.*;

public class SOMTaskTest {

    @Test
    public void jalur1() {

        SOMTaskMock somTaskMock = new SOMTaskMock();
        int width = 0;
        int height = 1;
        double[] sample = new double[]{0,0};
        double[][][] weight = new double[][][]{ { { 0,0 } } };
        int[] winner = somTaskMock.winningNeuron( width, height, sample, weight );

        Assert.assertArrayEquals( new int[]{0,0}, winner );

    }
    @Test
    public void jalur2() {

        SOMTaskMock somTaskMock = new SOMTaskMock();
        int width = 1;
        int height = 0;
        double[] sample = new double[]{0,0};
        double[][][] weight = new double[][][]{ { { 0,0 } } };
        int[] winner = somTaskMock.winningNeuron( width, height, sample, weight );

        Assert.assertArrayEquals( new int[]{0,0}, winner );

    }
    @Test
    public void jalur3() {

        SOMTaskMock somTaskMock = new SOMTaskMock();
        int width = 1;
        int height = 1;
        double[] sample = new double[]{Integer.MAX_VALUE, Integer.MAX_VALUE};
        double[][][] weight = new double[][][]{ { { 0,0 } } };
        int[] winner = somTaskMock.winningNeuron( width, height, sample, weight );

        Assert.assertArrayEquals( new int[]{0,0}, winner );

    }
    @Test
    public void jalur4() {

        SOMTaskMock somTaskMock = new SOMTaskMock();
        int width = 2;
        int height = 2;
        double[] sample = new double[]{1,1};
        double[][][] weight = new double[][][]{
                { { 0,0 }, { 0,0 } },
                { { 0,0 }, { 1,1 } }
        };
        int[] winner = somTaskMock.winningNeuron( width, height, sample, weight );

        Assert.assertArrayEquals( new int[]{1,1}, winner );

    }

    /*

    1   width=0

        winner = [0,0]

    2   width=1
        height=0

        winner = [0,0]

    3   width=1
        height=1

        winner = [0,0]

    4   width=1
        height=1
        sample=[0,0]
        weight[0][0]=[1,1]

        winner = [0,0]
    */

}

class SOMTaskMock extends SOMTask {

    public SOMTaskMock() {
        super( new double[1][1], new String[]{""}, 50 );
    }

    public int[] winningNeuron(
             int width,
             int height,
             double[] sample,
             double[][][] weight
    ) {

        double distance;
        double minDistance = Integer.MAX_VALUE;
        int[]  winner = new int[2];

        // looking for the neuron that has minimum distance to the actual sample data
        for (int k = 0; k < width; k++) {

            for (int l = 0; l < height; l++) {

                distance = euclidDistance( sample, weight[k][l] );

                System.out.println( "dist: "  + distance + " < mindist: " + minDistance );
                if ( distance < minDistance ) {
                    System.out.println( "dist is less than mindist" );
                    minDistance = distance;
                    winner[0] = k;
                    winner[1] = l;

                }

            }

        }

        return winner;

    }

    private double euclidDistance( double[] vecA, double[] vecB ) {

        double sum = 0;

        for( int i = 0; i < vecA.length; i++ ) {

            sum += pow( vecA[i] - vecB[i], 2 );

        }

        return sqrt(sum);

    }

}