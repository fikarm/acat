package acat.algorithm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// working version of doc2vec
public class Word2Vec {

    final int   MAX_STRING          =         100,
                MAX_CODE_LENGTH     =          40,
                MAX_SENTENCE_LENGTH =        1000,
                MAX_EXP             =           6,
                EXP_TABLE_SIZE      =        1000,
                VOCAB_HASH_SIZE     =  30_000_000,
                TABLE_SIZE          = 100_000_000;

    String  train_file,
            output_file,
            save_vocab_file,
            read_vocab_file;

    int     binary = 0,
            debug_mode = 2,
            window = 5,
            min_count = 5,
            num_threads = 12,
            min_reduce = 1,
            vocab_hash[] = new int[VOCAB_HASH_SIZE],
            table[],
            negative = 5;

    long    vocab_max_size = 1000,
            vocab_size = 0,
            layer1_size = 100,
            train_words = 0,
            word_count_actual = 0,
            iter = 5,
            file_size = 0,
            classes = 0;

    vocab_word vocab[] = new vocab_word[(int)vocab_max_size];

    boolean hs = false,
            cbow = true,
            feof, 
            isEOL;

    float   alpha = 0.025f,
            starting_alpha,
            sample = 1e-3f,
            syn0[],
            syn1[],
            syn1neg[],
            expTable[];

    public Word2Vec(){
        expTable = new float[EXP_TABLE_SIZE];
        for (int i = 0; i < EXP_TABLE_SIZE; i++) {
            expTable[i] = (float)Math.exp((i / (float)EXP_TABLE_SIZE * 2 - 1) * MAX_EXP);
            expTable[i] = expTable[i] / (expTable[i] + 1);
        }
    }


    void InitUnigramTable() {

        int a, i;
        double  train_words_pow = 0,
                power = 0.75,
                d1;

        table = new int[TABLE_SIZE];

        for (a = 0; a < vocab_size; a++)
            train_words_pow += Math.pow(vocab[a].cn, power);

        i = 0;
        d1 = Math.pow(vocab[i].cn, power) / train_words_pow;
        for (a = 0; a < TABLE_SIZE; a++) {

            table[a] = i;

            if (a / (double)TABLE_SIZE > d1) {
                i++;
                d1 += Math.pow(vocab[i].cn, power) / train_words_pow;
            }

            if (i >= vocab_size) {
                i = (int)(vocab_size - 1);
            }
        }
    }


    void ReadWord(char[] word, RandomAccessFile fin) throws IOException {
        int a = 0, ch;
        Arrays.fill(word, (char)0);

        word_loop: while ((ch = fin.read()) != -1) {

            // Check for word boundaries...
            switch(ch){
                case   13  : continue;
                case '\t'  : if (a > 0) break word_loop; else continue;
                case  ' '  : if (a > 0) break word_loop; else continue;
                case '\n'  : isEOL = true;
                             if (a > 0) break word_loop; else {
                                 word[0]='<';word[1]='/';word[2]='s';word[3]='>';
                                 return;
                             }
            }

            // If the character wasn't space, tab, CR, or newline, add it to the word.
            word[a++] = (char)ch;

            if (a >= MAX_STRING - 1)
                break;
        }

        if (ch == -1)
            feof = true;

    }


    long GetWordHash(String word) {
        long a, hash = 0; //unsigned long
        for (a = 0; a < word.length(); a++)
            hash = hash * 257 + word.charAt((int)a);
        hash = Long.remainderUnsigned(hash, VOCAB_HASH_SIZE);
        return hash;
    }


    int SearchVocab(char[] str) {

        String word = new String(str).trim();

        long hash = GetWordHash(word); // unsigned int
        while (true) {
            if (vocab_hash[(int)hash] == -1)
                return -1;

            if (word.equals(vocab[vocab_hash[(int)hash]].word))
                return vocab_hash[(int)hash];

            hash = Long.remainderUnsigned((hash + 1), VOCAB_HASH_SIZE);
        }
    }


    long ReadWordIndex(RandomAccessFile fin) throws IOException {
        char word[] = new char[MAX_STRING];
        ReadWord(word, fin);
        return SearchVocab(word);
    }


    int AddWordToVocab(char[] str) {
        String word = new String(str).trim();
        long hash; // unsigned

        vocab[(int)vocab_size] = new vocab_word(word);
        vocab_size++;

        if (vocab_size + 2 >= vocab_max_size)
            vocab = Arrays.copyOf(vocab, (int)(vocab_max_size += 1000));

        hash = GetWordHash(word);
        while (vocab_hash[(int)hash] != -1)
            hash = Long.remainderUnsigned((hash + 1), VOCAB_HASH_SIZE);
        vocab_hash[(int)hash] = (int)(vocab_size - 1);

        return (int)(vocab_size - 1);
    }


    void SortVocab() {
        int a, size;
        long hash;

        vocab = Arrays.copyOf(vocab, (int)vocab_size);
        Arrays.sort(vocab, 1, (int)vocab_size, new vocabCompare());

        Arrays.fill(vocab_hash, -1);

        size = (int)vocab_size;
        train_words = 0;

        for (a = 0; a < size; a++) {
            if ((vocab[a].cn < min_count) && (a != 0)) {
                vocab_size--;
                vocab[a] = null;
            } else {
                hash = GetWordHash(vocab[a].word);
                while (vocab_hash[(int)hash] != -1)
                    hash = Long.remainderUnsigned((hash + 1), VOCAB_HASH_SIZE);
                vocab_hash[(int)hash] = a;
                train_words += vocab[a].cn;
            }
        }

        vocab = Arrays.copyOf(vocab, (int)vocab_size);
    }


    void ReduceVocab() {
        int a, b = 0;
        long hash; // unsigned

        for (a = 0; a < vocab_size; a++) {
            if (vocab[a].cn > min_reduce) {
                vocab[b].cn = vocab[a].cn;
                vocab[b].word = vocab[a].word;
                b++;
            } else
                vocab[a] = null;
        }
        vocab_size = b;

        Arrays.fill(vocab_hash, -1);

        for (a = 0; a < vocab_size; a++) {
            hash = GetWordHash(vocab[a].word);
            while (vocab_hash[(int)hash] != -1)
                hash = Long.remainderUnsigned((hash + 1), VOCAB_HASH_SIZE);
            vocab_hash[(int)hash] = a;
        }

        min_reduce++;
    }


    void CreateBinaryTree() {
        long a, b, i, min1i, min2i, pos1, pos2, point[] = new long[MAX_CODE_LENGTH];
        byte code[] = new byte[MAX_CODE_LENGTH],
                binary[] = new byte[(int)(vocab_size * 2 + 1)];
        long count[] = new long[(int)(vocab_size * 2 + 1)],
                parent_node[] = new long[(int)(vocab_size * 2 + 1)];

        for (a = 0; a < vocab_size; a++)
            count[(int)a] = vocab[(int)a].cn;

        for (a = vocab_size; a < vocab_size * 2; a++)
            count[(int)a] = (long)1e15;

        pos1 = vocab_size - 1;
        pos2 = vocab_size;

        for (a = 0; a < vocab_size - 1; a++) {

            if (pos1 >= 0) {
                if (count[(int)pos1] < count[(int)pos2]) {
                    min1i = pos1;
                    pos1--;
                } else {
                    min1i = pos2;
                    pos2++;
                }
            } else {
                min1i = pos2;
                pos2++;
            }
            if (pos1 >= 0) {
                if (count[(int)pos1] < count[(int)pos2]) {
                    min2i = pos1;
                    pos1--;
                } else {
                    min2i = pos2;
                    pos2++;
                }
            } else {
                min2i = pos2;
                pos2++;
            }
            count[(int)(vocab_size + a)] = count[(int)min1i] + count[(int)min2i];
            parent_node[(int)min1i] = vocab_size + a;
            parent_node[(int)min2i] = vocab_size + a;
            binary[(int)min2i] = 1;

        }

        for (a = 0; a < vocab_size; a++) {
            b = a;
            i = 0;
            while (true) {
                code[(int)i] = binary[(int)b];
                point[(int)i] = b;

                i++;
                b = parent_node[(int)b];
                if (b == vocab_size * 2 - 2) break;
            }
            vocab[(int)a].codelen = (int)i;
            vocab[(int)a].point[0] = (int)(vocab_size - 2);
            for (b = 0; b < i; b++) {
                vocab[(int)a].code [(int)(i - b - 1)] = code[(int)b];
                vocab[(int)a].point[(int)(i - b)] = (int)(point[(int)b] - vocab_size);
            }
        }
    }


    void LearnVocabFromTrainFile() {
        char word[] = new char[MAX_STRING];
        long a, i;

        Arrays.fill(vocab_hash, -1);

        vocab_size = 0;

        AddWordToVocab("</s>".toCharArray());

        try( RandomAccessFile fin = new RandomAccessFile(train_file, "r")){
            while (true){
                if (isEOL) {
                    isEOL = false;
                    vocab[0].cn++;
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
            System.out.printf("Words in train file: %d\n", train_words);
        }
    }


    void SaveVocab() {
        try {
            List<String> lines = new ArrayList<>();
            for (vocab_word word : vocab)
                lines.add(String.format("%-20s %d", word.word, word.cn));

            Files.write(Paths.get(save_vocab_file), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void ReadVocab() {
        try(RandomAccessFile fin = new RandomAccessFile(read_vocab_file, "r")) {
            long a, i = 0;
            char word[] = new char[MAX_STRING];

            Arrays.fill(vocab_hash, -1);
            vocab_size = 0;

            while(true){
                ReadWord(word, fin);
                if(feof) break;
                a = AddWordToVocab(word);
                vocab[(int)a].cn = Integer.parseInt(fin.readLine().trim());
                i++;
            }

            SortVocab();

            if (debug_mode > 0) {
                System.out.printf("Vocab size: %d\n", vocab_size);
                System.out.printf("Words in train file: %d\n", train_words);
            }

            file_size = Files.size(Paths.get(train_file));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void InitNet() {
        long next_random = 1;

        syn0 = new float[(int)(vocab_size * layer1_size)];

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

        CreateBinaryTree();
    }


    void TrainModelThread(){
        long a, b, d, cw, word, last_word, sentence_length = 0, sentence_position = 0,
             word_count = 0, last_word_count = 0, sen[] = new long[MAX_SENTENCE_LENGTH + 1],
             l1, l2, c, target, label, local_iter = iter,
             next_random = 0;

        float f,
              g,
              neu1[]  = new float[(int)layer1_size],
              neu1e[] = new float[(int)layer1_size];

        try( RandomAccessFile fin = new RandomAccessFile(train_file, "r") ) {

            while(true){
                if (word_count - last_word_count > 10_000) {

                    word_count_actual += word_count - last_word_count;
                    last_word_count = word_count;

                    if (debug_mode > 0)
                        System.out.printf("Alpha: %f  Progress: %.2f%%  \r", alpha,
                                word_count_actual / (float)(iter * train_words + 1) * 100);

                    alpha = starting_alpha * (1 - word_count_actual / (float)(iter * train_words + 1));

                    float new_alpha = starting_alpha * 0.0001f;
                    if (alpha < new_alpha)
                        alpha = new_alpha;
                }

                if (sentence_length == 0) {
                    while (true) {
                        if (isEOL) {
                            isEOL = false;
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

                            if (sentence_length >= MAX_SENTENCE_LENGTH)
                                break;
                        }
                    }
                    sentence_position = 0;
                }

                if (feof || (word_count > train_words / num_threads)) {

                    feof = false;
                    word_count_actual += word_count - last_word_count;
                    local_iter--;
                    if (local_iter == 0) break;
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

                    if (cw > 0) {

                        for (c = 0; c < layer1_size; c++) neu1[(int)(c)] /= cw;

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


    void TrainModel() {

        System.out.printf("Starting training using file %s\n", train_file);

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

        try {

            List<String> lines = new ArrayList<>();
            lines.add(String.format("%d %d", vocab_size, layer1_size));

            for (int a = 0; a < vocab_size; a++) {
                String out = String.format("%-20s ", vocab[a].word);
                for (int b = 0; b < layer1_size; b++)
                    out += String.format("%10f ", syn0[(int) (a * layer1_size + b)]);
                lines.add(out);
            }
            Files.write(Paths.get(output_file), lines);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {

        Word2Vec wv = new Word2Vec();

        wv.train_file = "src/main/resources/dataset/corpus-db_util.txt";
        wv.save_vocab_file = "src/main/resources/output/vocab-db_util.txt";
        wv.output_file = "src/main/resources/output/wordvec-db_util.txt";
//        wv.read_vocab_file = wv.save_vocab_file;

        wv.layer1_size = 200;
        wv.window = 8;
        wv.iter = 15;
        wv.cbow = true;
        wv.alpha = 0.05f;
        wv.hs = false;
        wv.sample = 0;
        wv.negative = 0;
        wv.min_count = 1;
        wv.binary = 0;
        wv.debug_mode = 2;
        wv.num_threads = 1;

        wv.TrainModel();
    }

}

class vocab_word{
    int     MAX_CODE_LENGTH = 40,
            point[] = new int[MAX_CODE_LENGTH],
            codelen = 0,
            cn      = 0;
    byte    code [] = new byte[MAX_CODE_LENGTH];
    String word;

    public vocab_word(String word){
        this.word = word;
    }
}

class vocabCompare implements Comparator<vocab_word>{
    @Override
    public int compare(vocab_word a, vocab_word b) {
        return b.cn - a.cn;
    }
}