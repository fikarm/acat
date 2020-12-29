package acat.controller;

import acat.model.Entity;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;


public class Details extends IController{

    @FXML private VBox root;
    @FXML private TableView<Entity> tvCalcResult;
    @FXML private ToggleButton tbSC, tbCC, tbAC;

    @Override
    void load() {

        tbSC.setSelected(true);
        tbCC.setSelected(true);
        tbAC.setSelected(true);

        tbSC.selectedProperty().addListener((v, o, n) ->
                tvCalcResult.getColumns().stream().skip(1).forEach(col ->
                        col.getColumns().get(0).setVisible(n)));

        tbCC.selectedProperty().addListener((v, o, n) ->
                tvCalcResult.getColumns().stream().skip(1).forEach(col ->
                        col.getColumns().get(1).setVisible(n)));

        tbAC.selectedProperty().addListener((v, o, n) ->
                tvCalcResult.getColumns().stream().skip(1).forEach(col ->
                        col.getColumns().get(2).setVisible(n)));

        populateTable();

    }

    @Override
    Pane getRoot() {
        return root;
    }

    @Override
    String getTitle() {
        return "Details";
    }

    public void updateTable() {

        tvCalcResult.getColumns().clear();
        tvCalcResult.getItems().clear();
        isLoaded = false;

    }

    private void populateTable() {

        //File list column
        TableColumn<Entity, String> ttcFileList = new TableColumn<>("Class");
        ttcFileList.setCellValueFactory(param ->
                new ReadOnlyStringWrapper(param.getValue().toString())
        );
        ttcFileList.setSortable(false);
        tvCalcResult.getColumns().add(ttcFileList);

        //Coupling Result column loop
        for (int i = 0; i < main.entities.size(); i++) {

            Entity e = main.entities.get(i);

            TableColumn<Entity, String> tcGroup = new TableColumn<>(e.initial);
            TableColumn<Entity, String> ttcSC = new TableColumn<>("SC");
            TableColumn<Entity, String> ttcCC = new TableColumn<>("CC");
            TableColumn<Entity, String> ttcAC = new TableColumn<>("AC");


            final int index = i;
            ttcSC.setCellValueFactory(item -> new ReadOnlyStringWrapper(
                    String.format("%.3f",item.getValue().sc.get(index))));
            ttcCC.setCellValueFactory(item -> new ReadOnlyStringWrapper(
                    String.format("%.3f",item.getValue().cc.get(index))));
            ttcAC.setCellValueFactory(item -> new ReadOnlyStringWrapper(
                    String.format("%.3f",item.getValue().ac.get(index))));

            double width = 50;
            ttcSC.setPrefWidth(width);
            ttcCC.setPrefWidth(width);
            ttcAC.setPrefWidth(width);

            tcGroup.getColumns().add(ttcSC);
            tcGroup.getColumns().add(ttcCC);
            tcGroup.getColumns().add(ttcAC);

            ttcSC.setSortable(false);
            ttcCC.setSortable(false);
            ttcAC.setSortable(false);
            tcGroup.setSortable(false);

            tvCalcResult.getColumns().add(tcGroup);
        }

        tvCalcResult.getItems().addAll(main.entities);

    }

}
