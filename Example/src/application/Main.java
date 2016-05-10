package application;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
	List<Integer> boxes;
	BoxPlotWidget boxPlotWidget;
	
	@Override
	public void start(Stage primaryStage) {
		try {

			boxPlotWidget = new BoxPlotWidget(20, 20, 400, 400, 2000, BoxType.FIFO, 314, 0);
			Pane root = new Pane();
			root.getChildren().add(boxPlotWidget.getPane());
			Scene scene = new Scene(root, 600, 600);

			primaryStage.setScene(scene);
			primaryStage.setTitle("Well, fuck");
			primaryStage.show();
			primaryStage.setOnCloseRequest(e -> {
				boxPlotWidget.clean();
				Platform.exit();
			});
			
			Stage stage = new Stage();
			Button button = new Button("Add box");
			button.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					boxes.add(boxPlotWidget.addBox(50));
				}
			});
            stage.setScene(new Scene(new Group(button), 200, 200));
            stage.show();

			boxPlotWidget.setTranslateX(root.getPrefWidth() / 2);
			boxPlotWidget.setTranslateY(root.getPrefHeight() / 2);

			boxes = new Vector<>();
			boxes.add(boxPlotWidget.addBox(50));

			new Thread(new Runnable() {
				@Override
				public void run() {
					Random rand = new Random();
					while (true) {
						try {
							Thread.sleep(50);
							for (Integer integer : boxes) {
								boxPlotWidget.addData(integer, rand.nextDouble() * 314);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
