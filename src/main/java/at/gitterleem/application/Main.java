package at.gitterleem.application;


import at.gitterleem.application.core.scene.Scene;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		Scene scene = new Scene();

		BufferedImage img = scene.render(1024, 720);

		File file = new File("render.png");
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			System.out.println("Could now write file: " + file.getName());
		}
	}
}
