package acat.services;

import com.github.acat2.Acat;
import com.github.acat2.models.Entity;
import com.github.acat2.models.FileType;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FileVisitor extends SimpleFileVisitor<Path> {

    private final Image folderIcon = new Image (
            Acat.class.getResourceAsStream( "/images/icon-folder.png" )
    );
    private final Image javaClassIcon = new Image (
            Acat.class.getResourceAsStream( "/images/icon-java-class.png" )
    );
    private final Image otherFileIcon = new Image (
            Acat.class.getResourceAsStream( "/images/icon-other-file.png" )
    );

    private final List<CheckBoxTreeItem<Entity>> javaClassItemList = new ArrayList<>();
    private final CheckBoxTreeItem<Entity> root = new CheckBoxTreeItem<>();
    private CheckBoxTreeItem<Entity> pointer = root;

    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleBooleanProperty isCancelled = new SimpleBooleanProperty();
    private final Set<String> classNames = new HashSet<>();
    private final Set<String> classInitials = new HashSet<>();

    @Override
    public FileVisitResult preVisitDirectory( Path folderPath, BasicFileAttributes attrs ) {

        if ( isCancelled.get() ) return FileVisitResult.TERMINATE;

        updateMessage( "visit " + folderPath.getFileName().toString() );

        // membuat instansi tree item untuk folder
        ImageView iconImage = new ImageView( folderIcon );
        iconImage.setFitHeight( 22 );
        iconImage.setFitWidth( 22 );
        Entity folder = new Entity( folderPath.getFileName().toString(), FileType.FOLDER );
        CheckBoxTreeItem<Entity> folderItem = new CheckBoxTreeItem<>( folder, iconImage );
        folderItem.setExpanded( true );

        pointer.getChildren().add( folderItem );
        pointer = folderItem;
        return CONTINUE;

    }

    @Override
    public FileVisitResult visitFile ( Path filePath, BasicFileAttributes attrs ) throws IOException {

        if ( isCancelled.get() ) return FileVisitResult.TERMINATE;

        updateMessage( "visit " + filePath.getFileName().toString() );

        ImageView iconImage = new ImageView( );
        iconImage.setFitHeight( 22 );
        iconImage.setFitWidth( 22 );
        Entity entity;
        CheckBoxTreeItem<Entity> treeItem;
        String fileName = filePath.getFileName().toString();

        if ( fileName.endsWith( ".java" ) ) {

            // daftar kelas dalam satu file
            List<CheckBoxTreeItem<Entity>> javaClassItems = extractEntities( filePath );

            // jika lebih dari satu kelas dalam sebuah file, maka append kelas ke sebuah tree item parent
            if ( javaClassItems.size() == 0 ) {

                return CONTINUE;

            } else if ( javaClassItems.size() > 1 ) {

                iconImage.setImage( javaClassIcon );
                entity   = new Entity( fileName, FileType.JAVA_FILE );
                treeItem = new CheckBoxTreeItem<>( entity, iconImage );
                treeItem.getChildren().addAll( javaClassItems );

            } else

                treeItem = javaClassItems.get( 0 );

            treeItem.getValue().setHasJavaDescendant( true );

            // simpan referensi untuk item ini di List
            javaClassItemList.addAll(javaClassItems);

        } else {

            iconImage.setImage( otherFileIcon );
            entity   = new Entity( fileName, FileType.OTHER_FILE );
            treeItem = new CheckBoxTreeItem<>( entity, iconImage );

            // mencegah bug checkbox yang dapat dicentang meskipun dalam keadaan disable
            treeItem.addEventHandler(
                    CheckBoxTreeItem.checkBoxSelectionChangedEvent(),
                    event -> event.getTreeItem().setSelected( false )
            );

        }

        pointer.getChildren().add( treeItem );

        return CONTINUE;

    }

    @Override
    public FileVisitResult postVisitDirectory ( Path dir, IOException exc ) {

        if ( isCancelled.get() ) return FileVisitResult.TERMINATE;

        updateMessage( "processing " + dir.getFileName().toString() );

        // ambil daftar child dari pointer
        ObservableList<TreeItem<Entity>> pointerChildren = pointer.getChildren();

        // Operasi 1: Tentukan apakah pointer saat ini memiliki java descendant
        for (TreeItem<Entity> childItem : pointerChildren)

            if ( childItem.getValue().isHasJavaDescendant() ) {

                pointer.getValue().setHasJavaDescendant( true );
                break;

            }

        // Operasi 2: Cegah bug checkbox yang dapat dicentang meskipun dalam keadaan disable
        if ( !pointer.getValue().isHasJavaDescendant() )

            pointer.addEventHandler(
                    CheckBoxTreeItem.<Entity>checkBoxSelectionChangedEvent(),
                    event -> event.getTreeItem().setSelected( false )
            );

        // Operasi 3: Urutkan children dimana:
        // - folder yang punya file java berada di atas
        // - java file lebih dulu daripada other file
        pointerChildren.sort( Comparator
                .comparing( ( TreeItem<Entity> child ) -> !child.getValue().isHasJavaDescendant() )
                .thenComparing( ( TreeItem<Entity> child ) -> child.getValue().getFileType() )
        );

        // Operasi 4: Gabungkan child tunggal dengan parent-nya
        if ( pointerChildren.size() == 1 ) {

            CheckBoxTreeItem<Entity> pointerSingleChild = (CheckBoxTreeItem<Entity>) pointerChildren.get(0);

            if ( pointerSingleChild.getValue().getFileType() == FileType.FOLDER ) {

                // pindahkan anak dari single child ke pointer
                pointer.getChildren().setAll( pointerSingleChild.getChildren() );

                // gabungkan nama dari pointer dengan nama single child
                pointer.getValue().setName(
                        pointer.getValue() + "/" + pointerSingleChild.getValue()
                );

            }

        }

        // ubah referensi pointer ke parent-node-nya
        pointer = (CheckBoxTreeItem<Entity>) pointer.getParent();

        return CONTINUE;

    }


    private List<CheckBoxTreeItem<Entity>> extractEntities( Path javaFilePath ) throws IOException {

        // tempat menyimpan entity dari sebuah java file
        List<CheckBoxTreeItem<Entity>> entityItemList = new ArrayList<>();

        // set JavaParser agar bisa mem-parsing kode sumber dengan versi Java berapapun.
        StaticJavaParser.getConfiguration().setLanguageLevel(
                ParserConfiguration.LanguageLevel.RAW
        );

        // mengubah kode sumber java menjadi AST
        CompilationUnit cu = StaticJavaParser.parse( javaFilePath );

        // cari deklarasi package jika ada
        StringBuilder packageName = new StringBuilder();
        cu.findFirst(PackageDeclaration.class).ifPresentOrElse(
                javaPackage -> packageName.append( javaPackage.getNameAsString() ),
                () -> packageName.append( "default" )
        );

        // cari deklarasi kelas dalam satu file
        cu.findAll( ClassOrInterfaceDeclaration.class ).forEach( javaClass -> {

            // skip nested class, karena relevant properties nested class dijadikan satu dengan parent class
            if ( javaClass.isNestedType() ) return;

            // handle kelas dengan nama yang sama
            String className = javaClass.getNameAsString();
            if ( !classNames.add( className ) )
                className = packageName + "." + className;

            // handle kelas dengan nama inisial yang sama
            String classInitial = javaClass.getNameAsString().replaceAll( "([A-Z\\d])|.", "$1" );
            if ( !classInitials.add( classInitial ) )
                classInitial =
                        packageName.toString().replaceAll( "(^\\w|\\.\\w)|.", "$1" )
                                + "." + classInitial ;

            // membuat entitas untuk java class ini
            Entity entity = new Entity(
                className,
                classInitial,
                packageName.toString(),
                javaClass.toString(),
                javaClass.isInterface() ? "Interface" : javaClass.isAbstract() ? "Abstract class" : "Concrete class",
                javaClass.toString().lines().filter( s -> !s.isBlank() ).count(),
                extractRelevantProperties( javaClass )
            );

            // menyiapkan icon
            ImageView iconImage = new ImageView( javaClassIcon );
            iconImage.setFitHeight( 22 );
            iconImage.setFitWidth( 22 );

            // buat checkboxtreeitem dan tambahkan ke list
            entityItemList.add( new CheckBoxTreeItem<>( entity, iconImage ) );

        });

        return entityItemList;

    }

    private TreeSet<String> extractRelevantProperties( ClassOrInterfaceDeclaration javaClass ) {

        // Set menggunakan TreeSet agar menjaga urutan ketika menambahkan item
        TreeSet<String> relevantProperties = new TreeSet<>();

        // menambahkan nama kelas
        relevantProperties.add( javaClass.getNameAsString() );

        // menambahkan nama kelas yang di-extend
        javaClass.getExtendedTypes().forEach(
                ex -> relevantProperties.add( ex.asString() )
        );

        // menambahkan nama kelas yang di-implements
        javaClass.getImplementedTypes().forEach(
                im -> relevantProperties.add( im.asString() )
        );

        // menambahkan nama field
        javaClass.getFields().forEach(
                fi -> fi.getVariables().forEach(
                        v -> relevantProperties.add( "var_" + v.getNameAsString() )
                )
        );

        // menambahkan nama method
        javaClass.getMethods().forEach(
                me -> relevantProperties.add( me.getNameAsString() + "()" )
        );

        // jika ada nested class maka visit node secara rekursif,
        // barangkali di dalam nested class ada nested class
        for ( Node node : javaClass.getChildNodes() )

            if ( node instanceof ClassOrInterfaceDeclaration )

                relevantProperties.addAll(
                        extractRelevantProperties( (ClassOrInterfaceDeclaration) node )
                );

        return relevantProperties;

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

    public List<CheckBoxTreeItem<Entity>> getJavaClassItemList() {
        return javaClassItemList;
    }

    public CheckBoxTreeItem<Entity> getRoot() {

        root.setSelected(true);
        return root;

    }

}
