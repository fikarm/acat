package acat.controller;

import javafx.scene.layout.Pane;

abstract class IController {

    boolean isLoaded = false;
    Main main;

    void init(){

        main.lbTitle.setText(getTitle());
        getRoot().toFront();
        getRoot().setVisible(true);

        if(!isLoaded) {
            isLoaded = true;
            load();
        }

    }

    abstract void load();

    abstract Pane getRoot();

    abstract String getTitle();

}
