package acat.algorithm;

import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 *  SOM Implementation based on
 *  Haykin (1997)
 */
public class SOM {

    private double[][]         input;         // row = data, column = feature
    private double[][][]       weight;        // neuron's weight
    private int                width;
    private int                height;        // lattice width and height
    private int                inputSize;     // input vector size
    private double             radius;        // σ0
    private double             learningRate;  // η0
    private double             time1;         // τ1 = Eq.(9.6)  time constant
    private double             time2;         // τ2 = Eq.(9.14) time constant
    private long               random;        // random seed
    private int                left;
    private int                right;
    private int                top;
    private int                bottom;
    private String[]           tags;
    private Map<String, int[]> tagBMU;      // holds winning neurons tag
    public int                 maxIter;
    public IntegerProperty     iter;

    public SOM( double[][] input, String[] tags, int mapSize ) {

        this.input    = input;
        inputSize     = input[0].length;
        this.tags     = tags;
        this.tagBMU   = new HashMap<>();

        // populate tagBMU
        for ( String tag : tags ) tagBMU.put( tag, new int[2] );

        // som parameters
        width        = mapSize;
        height       = mapSize;
        maxIter      = 500 * width * height;
        radius       = width / 2.0;
        learningRate = 0.1;
        time1        = maxIter / log(radius);
        time2        = maxIter;
        random       = 1;

        // used to implement torus topology
        left         = ( width - 1 ) / 2;
        top          = ( height - 1 ) / 2;
        right        = ( width & 1 ) == 0 ? left + 1 : left;
        bottom       = ( height & 1 ) == 0 ? top + 1 : top;

        iter = new SimpleIntegerProperty( 0 );

    }

    /**
     * Starting point
     */
    public void run() {

        initWeight();

        double[] x;                  // input sample
        int      r;                  // input sample index
        int[]    i;                  // winning neuron's index x, y
        double   sigma;              // decaying radius
        double   eta = learningRate; // decaying learning rate, never below 0.01

        for ( iter.set( 0 ); ( iter.get() < maxIter) && ( eta >= 0.01 ); iter.set( iter.get() + 1 ) ) {

            r     = pickRandomSample();
            x     = input[r];
            i     = winningNeuron( x );
            sigma = decayRadius( iter.get() );
            eta   = decayLearningRate( iter.get() );

            updateWeight( x, i, sigma, eta );

            tagBMU.replace( tags[r], i );

        }

    }

    /**
     * Initialize neuron's synaptic weights randomly
     * between 0.0 and 1.0
     */
    private void initWeight() {

        weight = new double[width][height][inputSize];

        for (int k = 0; k < width; k++) {

            for (int l = 0; l < height; l++) {

                for (int m = 0; m < inputSize; m++) {

                    weight[k][l][m] = ( ( nextRandom() & 0xFFFF ) / 65536.0 );

                }

            }

        }

    }

    /**
     * Select input data randomly
     */
    private int pickRandomSample() {

        return (int) Long.remainderUnsigned( nextRandom(), input.length );

    }

    /**
     * Similarity match in terms of Euclidean Distance.
     *
     * Equation (9.3)
     * i(x) = arg min_j || x(n) - w_j ||,  j = 1,2,...,l
     */
    private int[] winningNeuron( double[] sample ) {

        double distance;
        double minDistance = Integer.MAX_VALUE;
        int[]  winner = new int[2];

        // looking for the neuron that has minimum distance to the actual sample data
        for (int k = 0; k < width; k++) {

            for (int l = 0; l < height; l++) {

                distance = euclidDistance( sample, weight[k][l] );

                if ( distance < minDistance ) {

                    minDistance = distance;
                    winner[0] = k;
                    winner[1] = l;

                }

            }

        }

        return winner;

    }

    /**
     * Lateral distance d_{j,i}
     * between winning neuron and some neuron
     * in terms of euclidean distance squared.
     * j = neuron's index [x,y]
     * i = winning neuron's index [x,y]
     *
     * Equation (9.5)
     * d_{j,i}^2 = || r_j - r_i ||^2
     */
    private double lateralDistance( int[] j, int[] i ) {

        return pow( j[0] - i[0], 2 ) + pow( j[1] - i[1], 2 );

    }

    /**
     * Decay radius σ exponentially
     * by the iterations n
     *
     * Equation (9.6)
     * σ(n) = σ_0 exp( -n / τ_1 ),  n = 0,1,2,...
     /*/
    private double decayRadius( int n ) {

        return radius * exp( -n / time1);

    }

    /**
     * Neighborhood function h_{j,i(x)}
     * in terms of gaussian distribution.
     *
     * Equation (9.7)
     * h_{j,i(x)}(n) = exp( -d_{j,i}^2 / 2 σ(n)^2 )
     /*/
    private double h( int[] j, int[] i, double sigmaSquared ) {

        return exp( -lateralDistance( j, i ) / ( 2 * sigmaSquared ) );

    }

    /**
     * Neurons weight update function
     * implemented with torus topology.
     * With torus topology, the top edge
     * will be connected to the bottom edge
     * while the left edge will be
     * connected to the right edge.
     *
     * Equation (9.13)
     * w_j(n+1) = w_j(n)+eta(n)*h_{j,i(x)}(n)*(x-w_j(n))
     /*/
    private void updateWeight( double[] x, int[] i, double sigma, double eta ) {

        // Untuk mengimplementasikan topologi torus,
        // loop tidak dimulai dari posisi x=0 y=0 pada lattice,
        // tetapi posisi x dan y ditentukan seperti seolah-olah
        // membuat lattice baru berukuran sama seperti sebelumnya
        // dengan bmu berada di tengah-tengahnya. Area lattice baru
        // tersebut yang akan dilakukan loop. Lattice baru tersebut
        // kemungkinan bisa mengalami offset dari lattice asli,
        // oleh karena itu x dan y diwrap agar tetap berada
        // di area lattice asli.

        int     startX = i[0] - left,
                stopX  = i[0] + right,
                startY = i[1] - top,
                stopY  = i[1] + bottom,
                wrapX,
                wrapY;
        int[]   j   = new int[2];
        double  scalar;

        sigma = pow( sigma, 2 );

        for( int k = startX; k <= stopX; k++ ) {

            for( int l = startY; l <= stopY; l++ ) {

                j[0]   = k;
                j[1]   = l;

                scalar = eta * h( j, i, sigma );

                wrapX = wrapX( k );
                wrapY = wrapY( l );

                for ( int m = 0; m < inputSize; m++ ) {

                    weight[wrapX][wrapY][m] += scalar * ( x[m] - weight[wrapX][wrapY][m] );

                }

            }

        }

    }

    /**
     * Decay learning rate η exponentially
     * by the iterations n.
     *
     * Equation (9.14)
     * η(n) = η_0 exp( -n / τ_2 ),  n = 0,1,2,...
     */
    private double decayLearningRate( int n ) {

        return learningRate * exp( -n / time2 );

    }

    /**
     * Distance map with unified matrix (u-matrix)
     * Implemented with hexagonal neighborhood.
     */
    public double[][] getUMatrix() {

        double[][] umatrix = new double[width][height];

        for ( int x = 0; x < width; x++ )

            for ( int y = 0; y < height; y++ ) {

                double sum = 0;

                for ( int[] i : getNeighborIndexes( x, y ) )

                    sum += euclidDistanceSquared( weight[x][y], weight[i[0]][i[1]] );

                umatrix[x][y] = sum / 6;

            }

        minMaxNorm( umatrix );

        return umatrix;

    }

    /**
     * Get indexes of tile immediate neighbors
     */
    private int[][] getNeighborIndexes( int xCenter, int yCenter ){

        int xPrev = wrapX( xCenter - 1 ), yPrev = wrapY( yCenter - 1 ),
                xNext = wrapX( xCenter + 1 ), yNext = wrapY( yCenter + 1 );

        if ( (yCenter & 1) == 0 )

            return new int[][]{
                    { xCenter, yPrev   }, // N : x     , y-1
                    { xNext  , yCenter }, // E : x + 1 , y
                    { xCenter, yNext   }, // S : x     , y+1
                    { xPrev  , yCenter }, // W : x - 1 , y
                    { xNext  , yPrev   }, // SW: x - 1 , y+1
                    { xNext  , yNext   }  // NW: x - 1 , y-1
            };

        else

            return new int[][]{
                    { xCenter, yPrev   }, // N : x     , y-1
                    { xNext  , yCenter }, // E : x + 1 , y
                    { xCenter, yNext   }, // S : x     , y+1
                    { xPrev  , yCenter }, // W : x - 1 , y
                    { xPrev  , yNext   }, // SW: x-1   , y+1
                    { xPrev  , yPrev   }  // NW: x-1   , y-1
            };

    }

    private void minMaxNorm( double[][] umatrix ) {

        double  min = 0,
                max = 0;

        for ( double[] row : umatrix )

            for (double col : row) {

                if ( col < min ) min = col;
                if ( col > max ) max = col;

            }

        for (int i = 0; i < umatrix.length; i++)

            for (int j = 0; j < umatrix[0].length; j++)

                umatrix[i][j] = ( umatrix[i][j] - min ) / ( max - min );

    }

    private double euclidDistance( double[] vecA, double[] vecB ) {

        double sum = 0;

        for( int i = 0; i < vecA.length; i++ ) {

            sum += pow( vecA[i] - vecB[i], 2 );

        }

        return sqrt(sum);

    }

    private double euclidDistanceSquared( double[] vecA, double[] vecB ) {

        double sum = 0;

        for( int i = 0; i < vecA.length; i++ ) {

            sum += pow( vecA[i] - vecB[i], 2 );

        }

        return sum;

    }

    private int wrapX( int x ) {

        if ( x < 0 || x > ( width - 1 ) )

            return abs ( abs(x) - width );

        else

            return x;

    }

    private int wrapY( int y ) {

        if ( y < 0 || y > ( height - 1 ) )

            return abs( abs(y) - height );

        else

            return y;

    }

    private long nextRandom() {

        return random = random * 25214903917L + 11;

    }

    public Map<String, int[]> getBMU() {

        return tagBMU;

    }

}
