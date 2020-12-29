package acat.services;

import com.github.acat2.models.Entity;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// TODO: mungkin iso digawe input + proses + output: matrix sc,cc,ac,ace
public class ACE {

    private final Collection<Entity> entities;
    private final double[][] pv;
    private final double w;
    private final String output;

    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isCancelled = new SimpleBooleanProperty();

    // pastikan collection mempertahankan order
    public ACE( Collection<Entity> entities, double[][] pv, double w, String output ) {

        this.entities = entities;
        this.pv = pv;
        this.w = w;
        this.output = output;
    }

    public void run() {

        int firstIndex = 0;
        int currentProgress = 0;
        int maxProgress = entities.size();

        for ( Entity firstEntity : entities ) {

            if ( isCancelled.get() ) return;
            
            // hapus perhitungan sc, cc, dan ac, dan ace sebelumnya
            firstEntity.getSc().clear();
            firstEntity.getCc().clear();
            firstEntity.getAc().clear();
            firstEntity.setAce( 0 );

            // menambahkan perhitungan sebelumnya
            Iterator<Entity> entityIterator = entities.iterator();

            // loop perhitungan SC, CC, dan AC dari firstEntity terhadap secondEntity
            for ( int secondIndex = 0; entityIterator.hasNext(); secondIndex++ ) {

                Entity secondEntity = entityIterator.next();
                double sc;
                double cc;
                double ac;

                if ( secondIndex == firstIndex ) {

                    sc = 1.0;
                    cc = 1.0;
                    ac = 1.0;

                } else if ( secondIndex < firstIndex ) {

                    // ambil kalkulasi kopling sebelumnya dari secondEntity
                    sc = secondEntity.getSc().get( firstIndex );
                    cc = secondEntity.getCc().get( firstIndex );
                    ac = secondEntity.getAc().get( firstIndex );

                } else {

                    // kalkulasi kopling e1 terhadap e2
                    sc = SC( firstEntity.getRp(), secondEntity.getRp() );
                    cc = CC( pv[ firstIndex ], pv[ secondIndex ] );
                    ac = AC( sc, cc, w );

                }

                firstEntity.getSc().add( sc );
                firstEntity.getCc().add( cc );
                firstEntity.getAc().add( ac );

                updateMessage( String.format(
                        "calculate aggregated coupling between %10s - %10s",
                        firstEntity, secondEntity
                ));

            }

            // kalkulasi ACE dari firstEntity
            firstEntity.setAce( ace( firstEntity.getAc() ) );

            firstIndex++;
            currentProgress++;

        }

        writeResult();

    }

    private double SC(Set<String> rpA, Set<String> rpB) {

        //Merge relevantPropertiesA and relevantPropertiesB
        Set<String> set = new LinkedHashSet<>();
        set.addAll(rpA);
        set.addAll(rpB);

        //Calculate Structural Coupling
        int unions = set.size();
        int intersections = rpA.size() + rpB.size() - unions;

        return ((double)intersections) / unions;

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

    private double AC(double sc, double cc, double w) {

        return (w * sc) + ((1 - w) * cc);

    }

    private double ace(List<Double> ac) {

        double sum = 0;

        for (double v : ac)
            sum += v;

        return --sum / (ac.size() - 1);

    }

    
    private void writeResult() {

        try {

            final String format = "%8s; %s", fformat = "%.20f";

            StringBuilder rp  = new StringBuilder();
            StringBuilder sc  = new StringBuilder();
            StringBuilder cc  = new StringBuilder();
            StringBuilder ac  = new StringBuilder();
            StringBuilder ace = new StringBuilder();

            entities.forEach(e -> {

                rp.append(String.format(format, e.getInitial(), e.getRp()
                        .stream()
                        .map(r -> String.format("%30s", r))
                        .collect(Collectors.joining("; "))));
                sc.append(String.format(format, e.getInitial(),e.getSc()
                        .stream()
                        .map(v -> String.format(fformat, v))
                        .collect(Collectors.joining("; "))));
                cc.append(String.format(
                        format,
                        e.getInitial(),
                        e.getCc().stream()
                                 .map(v -> String.format(fformat, v))
                                 .collect(Collectors.joining("; ")))
                );
                ac.append(String.format(
                        format,
                        e.getInitial(),
                        e.getAc().stream()
                                 .map(v -> String.format(fformat, v))
                                 .collect(Collectors.joining("; ")))
                );

                ace.append(String.format("%8s; %.20f", e.getInitial(), e.getAce()));

                rp.append("\n");
                sc.append("\n");
                cc.append("\n");
                ac.append("\n");
                ace.append("\n");

            });

            Files.write(Paths.get(output+ "/1-rp.txt"), rp.toString().getBytes());
            Files.write(Paths.get(output+ "/2-sc.txt"), sc.toString().getBytes());
            Files.write(Paths.get(output+ "/3-cc.txt"), cc.toString().getBytes());
            Files.write(Paths.get(output+ "/4-ac.txt"), ac.toString().getBytes());
            Files.write(Paths.get(output+ "/5-ace.txt"), ace.toString().getBytes());

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    private void updateMessage( String message ) {
        this.message.set( message );
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }

    public SimpleBooleanProperty isCancelledProperty() {
        return isCancelled;
    }

}
