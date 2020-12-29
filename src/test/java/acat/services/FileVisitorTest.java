package acat.services;

import com.github.acat2.models.Entity;
import com.github.acat2.models.FileType;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.junit.Assert.*;

public class FileVisitorTest {

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER ) )
     *      pointer.children = {}
     *      pointer.value.hasJavaDescendant = true
     *
     * Expected Result:
     *      pointer akan tercentang ketika dicentang
     */
    @Test
    public void jalur1() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
            new Entity( "parent_folder", FileType.FOLDER )
        );
        pointer.getValue().setHasJavaDescendant( true );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertTrue( pointer.isSelected() );

    }

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER ) )
     *      pointer.children = {}
     *      pointer.value.hasJavaDescendant = false
     *
     * Expected Result:
     *      pointer tidak akan tercentang meskipun dicentang
     */
    @Test
    public void jalur2() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
                new Entity( "parent_folder", FileType.FOLDER )
        );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertFalse( pointer.isSelected() );

    }

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER) )
     *      pointer.children = {
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_folder1", FileType.FOLDER ) )
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_folder2", FileType.FOLDER ) )
     *      }
     *      pointer.value.hasJavaDescendant = true
     *
     * Expected Result:
     *      pointer akan tercentang ketika dicentang
     */
    @Test
    public void jalur3() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
                new Entity( "parent_folder", FileType.FOLDER ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_folder1", FileType.FOLDER ) ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_folder2", FileType.FOLDER ) ) );
        pointer.getValue().setHasJavaDescendant( true );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertTrue( pointer.isSelected() );

    }

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER) )
     *      pointer.children = {
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_folder1", FileType.FOLDER) )
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_folder2", FileType.FOLDER) )
     *      }
     *      pointer.children[0].value.hasJavaDescendant = true
     *
     * Expected Result:
     *      pointer akan tercentang ketika dicentang
     */
    @Test
    public void jalur4() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
                new Entity( "parent_folder", FileType.FOLDER ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_folder1", FileType.FOLDER ) ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_folder2", FileType.FOLDER ) ) );
        pointer.getChildren().get( 0 ).getValue().setHasJavaDescendant( true );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertTrue( pointer.isSelected() );

    }

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER) )
     *      pointer.children = {
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_file1", FileType.OTHER_FILE) )
     *      }
     *      pointer.value.hasJavaDescendant = true
     *
     * Expected Result:
     *      pointer akan tercentang ketika dicentang
     *      nama pointer masih "parent_folder"
     */
    @Test
    public void jalur5() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
                new Entity( "parent_folder", FileType.FOLDER ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_file1", FileType.OTHER_FILE ) ) );
        pointer.getChildren().get( 0 ).getValue().setHasJavaDescendant( false );
        pointer.getValue().setHasJavaDescendant( true );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertTrue( pointer.isSelected() );
        Assert.assertEquals( "parent_folder", pointer.getValue().toString() );

    }

    /**
     * Input:
     *      pointer = new CheckBoxTreeItem<Entity>( new Entity( "parent_folder", FileType.FOLDER) )
     *      pointer.children = {
     *          new CheckBoxTreeItem<Entity>( new Entity( "child_folder", FileType.FOLDER ) )
     *      }
     *      pointer.value.hasJavaDescendant = true
     *
     * Expected Result:
     *      pointer akan tercentang apabila dicentang
     *      nama pointer menjadi "parent_folder/child_folder"
     */
    @Test
    public void jalur6() {

        CheckBoxTreeItem<Entity> pointer = new CheckBoxTreeItem<>(
                new Entity( "parent_folder", FileType.FOLDER ) );
        pointer.getChildren().add( new CheckBoxTreeItem<>(
                new Entity( "child_folder1", FileType.FOLDER ) ) );
        pointer.getValue().setHasJavaDescendant( true );

        FileVisitorMock fileVisitorMock = new FileVisitorMock( pointer );
        fileVisitorMock.postVisitDirectory( Paths.get(""), null );

        pointer.setSelected( true );
        Assert.assertTrue( pointer.isSelected() );
        Assert.assertEquals( "parent_folder/child_folder1", pointer.getValue().toString() );

    }

}

class FileVisitorMock extends FileVisitor {

    private CheckBoxTreeItem<Entity> pointer;

    public FileVisitorMock( CheckBoxTreeItem<Entity> pointer ) {

        this.pointer = pointer;

    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

        // ambil daftar child dari pointer
        ObservableList<TreeItem<Entity>> pointerChildren = pointer.getChildren();

        // Operasi 1: Tentukan apakah pointer saat ini memiliki java descendant
        for (TreeItem<Entity> childItem : pointerChildren) {
            System.out.println( "pointerChildren loop" );
            if (childItem.getValue().isHasJavaDescendant()) {
                System.out.println( "   pointer children does have java descendant" );
                pointer.getValue().setHasJavaDescendant(true);
                break;

            }
        }

        // Operasi 2: Cegah bug checkbox yang dapat dicentang meskipun dalam keadaan disable
        if ( !pointer.getValue().isHasJavaDescendant() ) {
            System.out.println( "attach event handler to prevent the pointer being checked ");
            pointer.addEventHandler(
                    CheckBoxTreeItem.<Entity>checkBoxSelectionChangedEvent(),
                    event -> event.getTreeItem().setSelected(false)
            );
        }
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
            System.out.println( "pointer has only one children" );
            if ( pointerSingleChild.getValue().getFileType() == FileType.FOLDER ) {
                System.out.println( "   move the grandchildren of the pointer as it children" );
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

}