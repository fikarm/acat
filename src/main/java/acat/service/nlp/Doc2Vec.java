package acat.service.nlp;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Doc2Vec extends Word2Vec{

    private int     doc_size;
    private float[] syn0doc;
    private String  doc_vec_output_file, output;
    public StringProperty message;

    public Doc2Vec(String input, String pvOutput, int layerSize, int window, int iterations,
                   float alpha, float subsample, int negativeSample, boolean useHS){

        this.layer1_size = layerSize;
        this.window      = window;
        this.iter        = iterations;
        this.cbow        = true;
        this.alpha       = alpha;
        this.hs          = useHS;
        this.sample      = subsample;
        this.negative    = negativeSample;
        this.min_count   = 1;
        this.binary      = 0;
        this.debug_mode  = 1;
        this.num_threads = 1;

        output              = Paths.get(pvOutput).getParent().toString();
        train_file          = input;
        save_vocab_file     = output + "/dv-vocab.txt";
        output_file         = output + "/wv-vector.txt";
        doc_vec_output_file = pvOutput;

        message = new SimpleStringProperty();

    }


    @Override
    void LearnVocabFromTrainFile() {
        char[] word = new char[MAX_STRING];
        long a, i;

        Arrays.fill(vocab_hash, -1);

        vocab_size = 0;

        AddWordToVocab("</s>".toCharArray());

        try( RandomAccessFile fin = new RandomAccessFile(train_file, "r")){
            while (true){
                if (isEOL) {
                    isEOL = false;
                    vocab[0].cn++;
                    /*docvec*/doc_size++;
                    continue;
                }

                ReadWord(word, fin);
                train_words++;

                if (feof) {
                    feof = false;
                    break;
                }

                if ((debug_mode > 1) && (train_words % 100000 == 0)) {
                    System.out.printf("%dK%c", train_words / 1000, 13);
                }

                i = SearchVocab(word);

                if (i == -1) {
                    a = AddWordToVocab(word);
                    vocab[(int)a].cn = 1;
                } else
                    vocab[(int)i].cn++;

                if (vocab_size > VOCAB_HASH_SIZE * 0.7)
                    ReduceVocab();
            }

            file_size = Files.size(Paths.get(train_file));

        } catch (IOException e) {
            e.printStackTrace();
        }

        SortVocab();

        if (debug_mode > 0) {
            System.out.printf("Vocab size: %d\n", vocab_size);
            /*docvec*/System.out.printf("Document size: %d\n", doc_size);
            System.out.printf("Words in train file: %d\n", train_words);
        }
    }

    @Override
    void InitNet() {
        long next_random = 1;

        syn0 = new float[(int)(vocab_size * layer1_size)];
        /*docvec*/syn0doc = new float[(int)(doc_size * layer1_size)];

        if (hs)
            syn1 = new float[(int)(vocab_size * layer1_size)];

        if (negative>0)
            syn1neg = new float[(int)(vocab_size * layer1_size)];

        for (int a = 0; a < vocab_size; a++) {
            for (int b = 0; b < layer1_size; b++) {
                next_random = next_random * 25214903917L + 11;
                syn0[(int)(a * layer1_size + b)] =
                        (float)(((next_random & 0xFFFF) / (float) 65536) - 0.5) / layer1_size;
            }
        }

        /*docvec*/
        for (int a = 0; a < doc_size; a++) {
            for (int b = 0; b < layer1_size; b++) {
                next_random = next_random * 25214903917L + 11;
                syn0doc[(int)(a * layer1_size + b)] =
                        (float)(((next_random & 0xFFFF) / (float) 65536) - 0.5) / layer1_size;
            }
        }
        /*docvec*/

        //save initial weight
        save_vector(syn0, (int)vocab_size, (int)layer1_size, "syn0.txt");
        save_vector(syn0doc, doc_size, (int)layer1_size, "syn0doc.txt");

        CreateBinaryTree();
    }

    @Override
    void TrainModelThread() {
        long    a, b, d, cw, word, last_word, sentence_length = 0, sentence_position = 0,
                word_count = 0, last_word_count = 0, sen[] = new long[MAX_SENTENCE_LENGTH + 1],
                l1, l2, c, target, label, local_iter = iter,
                next_random = 0,
                /*docvec*/doc_id = 0;

        float   f,
                g,
                neu1[]  = new float[(int)layer1_size],
                neu1e[] = new float[(int)layer1_size];

        

        try( RandomAccessFile fin = new RandomAccessFile(train_file, "r") ) {
            while(true){
                if (word_count - last_word_count > 10_000) {

                    word_count_actual += word_count - last_word_count;
                    last_word_count = word_count;

                    if (debug_mode > 0) {

                        System.out.printf("Alpha: %f  Progress: %.2f%%  \r", alpha,
                                word_count_actual / (float) (iter * train_words + 1) * 100);
                        message.set( String.format( "Doc2Vec | Alpha: %f  Progress: %.2f%%", alpha,
                                word_count_actual / (float) (iter * train_words + 1) * 100) );

                    }

                    alpha = starting_alpha * (1 - word_count_actual / (float)(iter * train_words + 1));

                    float new_alpha = starting_alpha * 0.0001f;
                    if (alpha < new_alpha)
                        alpha = new_alpha;
                }

                if (sentence_length == 0) {
                    /*docvec*/
                    if (isEOL) {
                        isEOL = false;
                        doc_id++;
                        
                    }


                    /*docvec*/
                    while (true) {
                        if (isEOL) {
                            // isEOL = false;
                            word_count++;
                            break;
                        }

                        if (feof)
                            break;

                        word = ReadWordIndex(fin);
                        if (word > -1) {
                            word_count++;

                            if (sample > 0) {
                                float term1 = (float)(Math.sqrt(vocab[(int) word].cn / (sample * train_words))) + 1;
                                float term2 = (sample * train_words) / vocab[(int) word].cn;
                                float ran   = term1 * term2;

                                next_random = next_random * 25214903917L + 11;

                                float threshold = (next_random & 0xFFFF) / (float) 65536;
                                if (ran < threshold) {
                                    continue;
                                }
                            }

                            sen[(int) sentence_length] = word;
                            sentence_length++;

                            

                            if (sentence_length >= MAX_SENTENCE_LENGTH) {
                                break;
                            }
                        }
                    }
                    sentence_position = 0;

                    
                    
                }

                if (feof || (word_count > train_words / num_threads)) {
                    word_count_actual += word_count - last_word_count;
                    local_iter--;
                    feof = false;

                    
                    
                    if (local_iter == 0) break;

                    /*docvec*/doc_id = 0;
                    word_count = 0;
                    last_word_count = 0;
                    sentence_length = 0;

                    fin.seek(0);

                    continue;
                }

                word = sen[(int)(sentence_position)];
                if (word == -1) continue;

                Arrays.fill(neu1, 0);
                Arrays.fill(neu1e, 0);

                next_random = next_random * 25214903917L + 11;
                b = Long.remainderUnsigned(next_random, window);

                if (cbow) {
                    cw = 0;

                    for (a = b; a < window * 2 + 1 - b; a++) {
                        if (a != window) {
                            c = sentence_position - window + a;
                            if (c < 0) continue;
                            if (c >= sentence_length) continue;

                            last_word = sen[(int) (c)];
                            if (last_word == -1) continue;

                            

                            for (c = 0; c < layer1_size; c++)
                                neu1[(int) (c)] += syn0[(int) (c + last_word * layer1_size)];

                            cw++;
                        }
                    }

                    /*docvec*/for (c = 0; c < layer1_size; c++)
                    /*docvec*/neu1[(int)(c)] += syn0doc[(int)(doc_id * layer1_size + c)];


                    if (cw > 0) {

                        for (c = 0; c < layer1_size; c++)
                            neu1[(int)(c)] /= /*docvec*/(cw + 1);

                        if (hs) {
                            for (d = 0; d < vocab[(int) (word)].codelen; d++) {
                                f = 0;
                                l2 = vocab[(int) (word)].point[(int) (d)] * layer1_size;

                                for (c = 0; c < layer1_size; c++) f += neu1[(int) (c)] * syn1[(int) (c + l2)];

                                if (f <= -MAX_EXP) continue;
                                else if (f >= MAX_EXP) continue;
                                else f = expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];

                                g = (1 - vocab[(int) (word)].code[(int) (d)] - f) * alpha;

                                for (c = 0; c < layer1_size; c++) neu1e[(int) (c)] += g * syn1[(int) (c + l2)];

                                for (c = 0; c < layer1_size; c++) syn1[(int) (c + l2)] += g * neu1[(int) (c)];

                            }
                        }

                        if (negative > 0) {
                            for (d = 0; d < negative + 1; d++) {

                                if (d == 0) {
                                    target = word;
                                    label = 1;
                                } else {
                                    next_random = next_random * 25214903917L + 11;

                                    target = table[(int)(Long.remainderUnsigned((next_random >>> 16), TABLE_SIZE))];

                                    if (target == 0)
                                        target = Long.remainderUnsigned(next_random, (vocab_size - 1)) + 1;

                                    if (target == word) continue;

                                    label = 0;
                                }

                                l2 = target * layer1_size;

                                f = 0;
                                for (c = 0; c < layer1_size; c++)
                                    f += neu1[(int)(c)] * syn1neg[(int)(c + l2)];

                                if (f > MAX_EXP) g = (label - 1) * alpha;
                                else if (f < -MAX_EXP) g = (label - 0) * alpha;
                                else g = (label - expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;

                                for (c = 0; c < layer1_size; c++)
                                    neu1e[(int)(c)] += g * syn1neg[(int)(c + l2)];

                                for (c = 0; c < layer1_size; c++)
                                    syn1neg[(int)(c + l2)] += g * neu1[(int)(c)];

                            }
                        }

                        for (a = b; a < window * 2 + 1 - b; a++) {
                            if (a != window) {
                                c = sentence_position - window + a;
                                if (c < 0) continue;
                                if (c >= sentence_length) continue;

                                last_word = sen[(int) (c)];
                                if (last_word == -1) continue;

                                for (c = 0; c < layer1_size; c++)
                                    syn0[(int) (c + last_word * layer1_size)] += neu1e[(int) (c)];

                            }
                        }

                        /*docvec*/for (c = 0; c < layer1_size; c++)
                        /*docvec*/syn0doc[(int)(doc_id * layer1_size + c)] += neu1e[(int)(c)];

//                        
                        
                        
                        
                        
                        
                        
                        
                        
                        

                    }
                }
                else {
                    for (a = b; a < window * 2 + 1 - b; a++) {
                        if (a != window) {
                            c = sentence_position - window + a;
                            if (c < 0) continue;
                            if (c >= sentence_length) continue;

                            last_word = sen[(int) (c)];

                            if (last_word == -1) continue;

                            l1 = last_word * layer1_size;

                            for (c = 0; c < layer1_size; c++) neu1e[(int) (c)] = 0;

                            if (hs) for (d = 0; d < vocab[(int) (word)].codelen; d++) {
                                f = 0;
                                l2 = vocab[(int) (word)].point[(int) (d)] * layer1_size;

                                for (c = 0; c < layer1_size; c++) f += syn0[(int) (c + l1)] * syn1[(int) (c + l2)];
                                if (f <= -MAX_EXP) continue;
                                else if (f >= MAX_EXP) continue;
                                else f = expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];

                                g = (1 - vocab[(int) (word)].code[(int) (d)] - f) * alpha;

                                for (c = 0; c < layer1_size; c++) neu1e[(int) (c)] += g * syn1[(int) (c + l2)];

                                for (c = 0; c < layer1_size; c++) syn1[(int) (c + l2)] += g * syn0[(int) (c + l1)];
                            }

                            if (negative > 0) for (d = 0; d < negative + 1; d++) {

                                if (d == 0) {
                                    target = word;
                                    label = 1;

                                } else {


                                    next_random = next_random * 25214903917L + 11;


                                    target = table[(int) ((next_random >>> 16) % TABLE_SIZE)];


                                    if (target == 0) target = next_random % (vocab_size - 1) + 1;


                                    if (target == word) continue;


                                    label = 0;
                                }

                                l2 = target * layer1_size;

                                f = 0;
                                for (c = 0; c < layer1_size; c++) f += syn0[(int) (c + l1)] * syn1neg[(int) (c + l2)];

                                if (f > MAX_EXP) g = (label - 1) * alpha;
                                else if (f < -MAX_EXP) g = (label - 0) * alpha;
                                else
                                    g = (label - expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;

                                for (c = 0; c < layer1_size; c++)
                                    neu1e[(int) (c)] += g * syn1neg[(int) ((int) (c + l2))];

                                for (c = 0; c < layer1_size; c++)
                                    syn1neg[(int) ((int) (c + l2))] += g * syn0[(int) ((int) (c + l1))];
                            }

                            for (c = 0; c < layer1_size; c++) syn0[(int) ((int) (c + l1))] += neu1e[(int) (c)];
                        }
                    }
                }

                sentence_position++;

                
                
                
                
                
                

                if (sentence_position >= sentence_length) {
                    sentence_length = 0;
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void TrainModel() {

        try {

            System.out.printf("Starting training using file %s\n", train_file);
            message.set( String.format( "Start training using file %s\n", train_file ) );


            starting_alpha = alpha;


            if (read_vocab_file != null)

                ReadVocab();

            else

                LearnVocabFromTrainFile();


            if (!save_vocab_file.isEmpty())

                SaveVocab();


            if (output_file.isEmpty())

                return;


            InitNet();


            if (negative > 0)

                InitUnigramTable();


            TrainModelThread();


            if (binary > 0) {

                try (
                    FileOutputStream fout = new FileOutputStream(doc_vec_output_file);
                    ObjectOutputStream oos = new ObjectOutputStream(fout)
                ) {

                    oos.writeObject(syn0doc);

                } catch (Exception ex) {

                    ex.printStackTrace();

                }

            } else {

                List<String> lines = new ArrayList<>();
                lines.add(String.format("%d %d", doc_size, layer1_size));

                for (int a = 0; a < doc_size; a++) {

                    StringBuilder out = new StringBuilder(String.format("%3d ", a));

                    for (int b = 0; b < layer1_size; b++)

                        out.append(String.format("%10f ", syn0doc[(int) (a * layer1_size + b)]));

                    lines.add(out.toString());
                }

                Files.write(Paths.get(doc_vec_output_file), lines);

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public double[][] getVector(){
        double[][] val = new double[doc_size][(int)layer1_size];
        for (int i = 0; i < doc_size; i++) {
            for (int j = 0; j < layer1_size; j++) {
                val[i][j] = syn0doc[(int)(i*layer1_size + j)];
            }
        }
        return val;
    }

    private void save_vector(float[] vector, int rows, int cols, String name) {

        try {

            StringBuilder sb = new StringBuilder();

            for (int a = 0; a < rows; a++) {
                for (int b = 0; b < cols; b++) {
                    sb.append(String.format("%+f; ", vector[(int)(a * layer1_size + b)]));
                }
                sb.append("\n");
            }

            Files.write(Paths.get(output + "/" +  name), sb.toString().getBytes());

        } catch (IOException e) {

            e.printStackTrace();

        }

    }
}