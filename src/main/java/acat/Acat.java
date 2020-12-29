package acat;

import com.github.acat2.controllers.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class Acat extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Memuat file frame.fxml
        URL mainFxml = Acat.class.getResource( "/views/frame.fxml" );
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation( mainFxml );
        Parent root = loader.load();

        // Memberikan referensi primaryStage ke MainController
        // bertujuan untuk menampilkan DirectoryChooser dan Alert.
        MainController mainController = loader.getController();
        mainController.setStage(primaryStage);

        primaryStage.setOnCloseRequest( event -> {
            mainController.dispose();
            Platform.exit();
            System.exit( 0 );
        });
        primaryStage.setTitle( "Aggregated Coupling Assessment Tool v1.0-SNAPSHOT" );
        primaryStage.setScene( new Scene( root ) );
        primaryStage.getIcons().add( new Image("images/logo-acat.png") );
        primaryStage.setMaximized( true );
        primaryStage.show();


        /*--- mencoba binding
        ObservableList<String> publisher = FXCollections.observableArrayList();
        TableColumn<String, String> col = new TableColumn<>("Items");
        col.setCellValueFactory( param -> new SimpleStringProperty( param.getValue() ) );
        TableView<String> tv = new TableView<>( publisher );
        tv.getColumns().add( col );

        AtomicInteger increment = new AtomicInteger(0);
        Button tambah = new Button("tambah");
        Button hapus = new Button("hapus");
        tambah.setOnAction( event -> publisher.add( "fakeItem-" + increment.getAndIncrement() ));
        hapus.setOnAction( event ->{ if (publisher.size() > 0) { publisher.remove(publisher.size()-1); increment.decrementAndGet();} });

        primaryStage.setScene( new Scene( new VBox( tambah, hapus, tv ) ) );
        primaryStage.show();*/

        /*--- coba bitmask
        int paramsValidity = 0b1101111;
        int pos = 3;
        int mask = 0b1;

        System.out.println( String.format("%7s", Integer.toBinaryString( paramsValidity )).replace(' ', '0') );
        System.out.println( String.format("%7s", Integer.toBinaryString( mask )).replace(' ', '0') );
        System.out.println( String.format("%7s", Integer.toBinaryString( mask << pos)).replace(' ', '0') );
        System.out.println( String.format("%7s", Integer.toBinaryString( mask << (pos+1))).replace(' ', '0') );
        System.out.println( String.format("%7s", Integer.toBinaryString( (0b111 + 1) & 0b111).replace(' ', '0') ) );*/

        /*--- coba validasi input
        System.out.println(Double.parseDouble("5"));
        System.out.println(Double.parseDouble("5-"));
        System.out.println(Double.parseDouble("5sa"));
        System.out.println(Double.parseDouble("5e2"));
        System.out.println(Double.parseDouble("5e-2"));*/

        /*--- coba sorting 3 list
        List<String> dummy = Arrays.asList("var_variable", "method()", "class",
                "var_variable", "method()", "class",
                "var_variable", "method()", "class",
                "var_variable", "method()", "class",
                "var_variable", "method()", "class");

        dummy.sort( (s1, s2) -> {

            boolean isVar1 = s1.startsWith("var_");
            boolean isVar2 = s2.startsWith("var_");
            boolean isFun1 = s1.endsWith("()");
            boolean isFun2 = s2.endsWith("()");

            if ( isVar1 )
                if ( isVar2 ) return 0;
                else if ( isFun2 ) return -1;
                else return 1;
            else if ( isFun1 )
                if ( isVar2 ) return 1;
                else if ( isFun2 ) return 0;
                else return 1;
            else
                if ( isVar2 ) return -1;
                else if ( isFun2 ) return -1;
                else return 0;

        } );
        dummy.forEach(System.out::println);*/

        /*Doc2Vec pv = new Doc2Vec(
                "/media/share/data/kuliah_s1/Semester_8/skripsi/leap3/samples/clean-corpus-dbutils-1.3.txt",
                "/media/share/data/kuliah_s1/Semester_8/skripsi/leap3/samples/",
                300,
                5,
                100,
                0.05f,
                1e-5f,
                20,
                true
        );
        pv.TrainModel();*/

//        System.exit(0);

    }

}
