package acat.service;

import acat.model.Entity;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ACETask extends Task<Double> {

    private List<Entity> entities;
    private double w, pv[][];
    private String output;

    public ACETask(List<Entity> entities, String inputPV, String outputAC, double w) {

        this.entities = entities;
        this.w = w;
        this.pv = readInput(inputPV);
        this.output = outputAC;

    }

    public ACETask(List<Entity> entities, double[][] inputPV, String outputAC, double w) {

        this.entities = entities;
        this.w = w;
        this.pv = inputPV;
        this.output = outputAC;

    }


    @Override
    protected Double call() {

        // set RP
        entities.forEach(this::RP);

        int n = entities.size();

        for (int i = 0; i < n; i++) {

            Entity e1 = entities.get(i);
            e1.sc.clear();
            e1.cc.clear();
            e1.ac.clear();
            e1.ace = -1;

            //add calculation before
            for (int j = 0; j < i; j++) {

                Entity e2 = entities.get(j);
                e1.sc.add(e2.sc.get(i));
                e1.cc.add(e2.cc.get(i));
                e1.ac.add(e2.ac.get(i));

            }

            // to itself
            e1.sc.add(1.0);
            e1.cc.add(1.0);
            e1.ac.add(1.0);

            // remaining entities
            for (int j = i + 1; j < n; j++) {

                Entity e2 = entities.get(j);
                double sc = SC( e1.rp, e2.rp ),
                       cc = CC( pv[i], pv[j] ),
                       ac = AC( sc, cc );

                e1.sc.add(sc);
                e1.cc.add(cc);
                e1.ac.add(ac);

            }

            e1.ace = ACE(e1.ac);

        }

        updateMessage("writing result to " + output);

        saveResult();

        updateMessage("analyse done.");

        return 0.;
    }

    private void RP(Entity e) {

        try {

            updateMessage("relevant properties of "+e.initial);
            updateMessage("parse file "+e.initial);

            Set<String> relevantProperties = new HashSet<>();

            StaticJavaParser
                    .getConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

            StaticJavaParser
                    .parse(e.getFile())
                    .findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(javaClass -> {

                        relevantProperties.add(javaClass.getNameAsString());

                        javaClass.getExtendedTypes().forEach(ex ->
                                relevantProperties.add(ex.getNameAsString()));

                        javaClass.getImplementedTypes().forEach(im ->
                                relevantProperties.add(im.getNameAsString()));

                        javaClass.getFields().forEach(fi ->
                                fi.getVariables().forEach(v ->
                                        relevantProperties.add("var_" + v.getNameAsString())));

                        javaClass.getMethods().forEach(me ->
                                relevantProperties.add(me.getNameAsString() + "()"));

                    });

            e.rp = relevantProperties;

        } catch (FileNotFoundException ex) {

            ex.printStackTrace();

        }

    }

    private double SC(Set<String> rpA, Set<String> rpB) {

        updateMessage("merge rpA and rpB in a set");
        //Merge relevantPropertiesA and relevantPropertiesB
        Set<String> set = new LinkedHashSet<>();
        set.addAll(rpA);
        set.addAll(rpB);
        updateMessage("merge done");


        updateMessage("calc the strucutral coupling");
        //Calculate Structural Coupling
        int unions = set.size();
        int intersections = rpA.size() + rpB.size() - unions;
        double structuralCoupling = ((double)intersections) / unions;
        updateMessage("calc sc done");


        return structuralCoupling;

    }

    private double CC(double[] pv1, double[] pv2) {

        double dot    = 0,
               normA  = 0,
               normB  = 0;

        for (int i = 0; i < pv1.length; i++) {

            dot   += pv1[i]*pv2[i];

            normA += Math.pow(pv1[i], 2);

            normB += Math.pow(pv2[i], 2);

        }

        return Math.abs(dot) / ( Math.sqrt(normA) * Math.sqrt(normB) );

    }

    private double AC(double sc, double cc) {

        return (w * sc) + ((1 - w) * cc);

    }

    private double ACE(List<Double> ac){

        double sum = 0;

        for (double v : ac)
            sum += v;

        return --sum / (entities.size() - 1);

    }


    public double[][] getAC(){

        int n = entities.size();
        double[][] ac = new double[n][n];

        for (int i = 0; i < n; i++)

            for (int j = 0; j < n; j++)

                ac[i][j] = entities.get(i).ac.get(j);

        return ac;

    }

    private double[][] readInput(String inputpv) {

        try {

            // read files
            List<String[]> input = Files.readAllLines(Paths.get(inputpv))
                                         .stream()
                                         .map(s -> s.split("\\s+"))
                                         .collect(Collectors.toList());

            // first line
            String[] firstLine = input.get(0);
            int nrow = Integer.parseInt(firstLine[0]);
            int ncol = Integer.parseInt(firstLine[1]);

            // convert to array
            double[][] pv = new double[nrow][ncol];

            for (int row = 0; row < nrow; row++)

                for (int col = 0; col < ncol; col++)

                    pv[row][col] = Double.parseDouble(input.get(row + 1)[col + 1]);

            return pv;

        } catch (IOException e) {

            e.printStackTrace();
            return null;

        }

    }

    // logging, have to be private
    private void saveResult() {

        String outputdir = Paths.get(output).getParent().toString();

        try {

            final String format = "%8s; %s", fformat = "%.20f";

            StringBuilder rp  = new StringBuilder();
            StringBuilder sc  = new StringBuilder();
            StringBuilder cc  = new StringBuilder();
            StringBuilder ac  = new StringBuilder();
            StringBuilder ace = new StringBuilder();

            entities.forEach(e -> {

                rp.append(String.format(format, e.initial, e.rp
                        .stream()
                        .map(r -> String.format("%30s", r))
                        .collect(Collectors.joining("; "))));

                sc.append(String.format(format, e.initial,e.sc
                        .stream()
                        .map(v -> String.format(fformat, v))
                        .collect(Collectors.joining("; "))));

                cc.append(String.format(format, e.initial, e.cc
                        .stream()
                        .map(v -> String.format(fformat, v))
                        .collect(Collectors.joining("; "))));

                ac.append(String.format(format, e.initial, e.ac
                        .stream()
                        .map(v -> String.format(fformat, v))
                        .collect(Collectors.joining("; "))));

                ace.append(String.format("%8s; %.20f", e.initial, e.ace));

                rp.append("\n");
                sc.append("\n");
                cc.append("\n");
                ac.append("\n");
                ace.append("\n");

            });

            Files.write(Paths.get(outputdir+ "/rp.txt"), rp.toString().getBytes());
            Files.write(Paths.get(outputdir+ "/sc.txt"), sc.toString().getBytes());
            Files.write(Paths.get(outputdir+ "/cc.txt"), cc.toString().getBytes());
            Files.write(Paths.get(output), ac.toString().getBytes());
            Files.write(Paths.get(outputdir+ "/ace.txt"), ace.toString().getBytes());

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}
