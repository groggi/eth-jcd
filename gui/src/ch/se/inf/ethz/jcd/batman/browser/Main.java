package ch.se.inf.ethz.jcd.batman.browser;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Batman Virtual Disk Browser");

		Browser ui = new Browser(primaryStage);
		Scene scene = new Scene(ui, 1200, 800);
		scene.getStylesheets().add(
				"/ch/se/inf/ethz/jcd/batman/browser/main.css");

		primaryStage.setScene(scene);

		ui.initialize();

		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
