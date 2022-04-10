package at.gitterleem.application.core.scene;


import at.gitterleem.application.core.math.Vec3f;
import at.gitterleem.application.core.primitive.Triangle;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Scene {

	private static final float PI = 3.141592654f;

	private ConcurrentLinkedDeque<Chunk> chunks;

	private final Vec3f bgColor = new Vec3f(0, 0, 0);

	private final Random random;

	List<Triangle> triangles = new ArrayList<>();

	public Scene() {
		long seed = System.nanoTime();
		System.out.println("\t> seed: " + seed);
		random = new Random(seed);

		setupScene();
	}

	public BufferedImage render(int width, int height) {
		long startTime = System.nanoTime();

		chunks = new ConcurrentLinkedDeque<>();

		// split into chunks of 100 x 100 px;
		for (int x = 0; x < width; x+= 100) {
			for (int y = 0; y < height; y+= 100) {
				try {
					chunks.add(new Chunk(x, Math.min(x + 100, width), y, Math.min(y + 100, height)));
				} catch (IllegalAccessError e) {
					e.printStackTrace();
				}
			}
		}


		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("\t> available cores: " + cores);
		int threadCount = cores - 2;
		System.out.println("\t> will render using: " + threadCount + " threads");

		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

		List<Thread> threads = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			Thread t = new Thread(() -> {
				while(!chunks.isEmpty()) {
					Chunk chunk = chunks.poll();

					for (int x = chunk.getXStart(); x < chunk.getXEnd(); x++) {
						for (int y = chunk.getYStart(); y < chunk.getYEnd(); y++) {
							img.setRGB(x, y, renderPixel(x, y, width, height).getRGB());
						}
					}
				}
			});
			threads.add(t);
			t.start();
		}

		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Duration duration = Duration.ofNanos(System.nanoTime() - startTime);

		System.out.println("\t> Rendering finished.\n\t\t>Took: " + duration.toHoursPart() + "h " + duration.toMinutesPart() + "min " + duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms " + duration.toNanosPart() + "ns");

		showImage(img);

		return img;
	}

	private Color renderPixel(int x, int y, int width, int height) {
		float fov = 80;
		float aspect = (float) width / height;
		float scale = (float) Math.tan(Math.toRadians(fov * 0.5));

		Vec3f color = new Vec3f();

		int subSamples = 48;

		for (int sample = 0; sample < subSamples; sample++) {
			float px = (2f * ((x + random.nextFloat()) / width) - 1f) * aspect * scale;
			float py = (1f - 2f * ((y + random.nextFloat()) / height)) * scale;

			Vec3f origin = new Vec3f(0, 1, 2.3f);
			Vec3f direction = new Vec3f(px, py, -1).normalize();

			color.add(radiance(origin, direction, 1).div(subSamples));
		}

		color.clamp(0, 1);

		int r = Math.round(color.x * 255f);
		int g = Math.round(color.y * 255f);
		int b = Math.round(color.z * 255f);

		return new Color(r, g, b);
	}

	Vec3f radiance(Vec3f rayOrigin, Vec3f rayDirection, int depth) {
		Vec3f intersect = new Vec3f();
		Triangle triangle = intersectScene(rayOrigin, rayDirection, intersect, depth);

		// ray missed, return background color
		if (triangle == null) {
			return bgColor;
		}

		Vec3f f = new Vec3f(triangle.getColor());
		float p = triangle.getColor().max();

		// russian roulette
		if (++depth > 6) {
			if (random.nextFloat() < p) {
				f.mul(1f / p);
			} else {
				return triangle.getEmission() != null ? triangle.getEmission() : new Vec3f(0, 0, 0);
			}
		}

		// diffuse
		Vec3f diffDir = randomVector(triangle.getNormal());
		Vec3f diffOrigin = Vec3f.add(intersect, Vec3f.mul(triangle.getNormal(), 0.00001f)); // Vec3f.add(intersect, Vec3f.mul(diffDir, 0.00001f));

		Vec3f emission = triangle.getEmission() != null ? new Vec3f(triangle.getEmission()) : new Vec3f(0, 0, 0);

		return emission.add(f.mul(radiance(diffOrigin, diffDir, depth)));
	}

	/*
	   if (obj.refl == DIFF){                  // Ideal DIFFUSE reflection
	     double r1=2*M_PI*erand48(Xi), r2=erand48(Xi), r2s=sqrt(r2);
	     Vec w=nl, u=((fabs(w.x)>.1?Vec(0,1):Vec(1))%w).norm(), v=w%u;
	     Vec d = (u*cos(r1)*r2s + v*sin(r1)*r2s + w*sqrt(1-r2)).norm();
	     return obj.e + f.mult(radiance(Ray(x,d),depth,Xi));
	    }
	 */

	private Triangle intersectScene(Vec3f rayOrigin, Vec3f rayDirection, Vec3f outIntersect, int depth) {
		float minDistance = Float.MAX_VALUE;

		Triangle triangle = null;

		// ignore front wall on first intersect, so the ray can pass through
		for (int i = (depth == 1 ? 2 : 0); i < triangles.size(); i++) {
			Triangle t = triangles.get(i);
			Vec3f intersect = new Vec3f();
			if (t.intersect(rayOrigin, rayDirection, intersect)) {
				float distance = Vec3f.sub(intersect, rayOrigin).length();

				if (distance < minDistance) {
					minDistance = distance;
					outIntersect.set(intersect);
					triangle = t;
				}
			}
		}

		return triangle;
	}

	private Vec3f randomVector(Vec3f normal) {
		float r1 = 2 * PI * random.nextFloat();
		float r2 = random.nextFloat();
		float r2s = (float) Math.sqrt(r2);

		Vec3f w = new Vec3f(normal);
		Vec3f u = (Math.abs(w.x) > .1 ? new Vec3f(0, 1, 0) : new Vec3f(1, 0, 0)).cross(w).normalize();
		Vec3f v = Vec3f.cross(w, u);

		return (u.mul((float) Math.cos(r1) * r2s)
				.add(v.mul((float) Math.sin(r1) * r2s))
				.add(w.mul((float) Math.sqrt(1f - r2)))).normalize();
	}

	private void showImage(BufferedImage image) {
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(image)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void setupScene() {
		// front wall
		triangles.add(new Triangle(
				new Vec3f(-1, 0, 1),
				new Vec3f(-1, 2, 1),
				new Vec3f(1, 0, 1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(1, 0, 1),
				new Vec3f(-1, 2, 1),
				new Vec3f(1, 2, 1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));

		// floor
		triangles.add(new Triangle(
				new Vec3f(-1, 0, 1),
				new Vec3f(1, 0, -1),
				new Vec3f(-1, 0, -1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-1, 0, 1),
				new Vec3f(1, 0, 1),
				new Vec3f(1, 0, -1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));

		// back wall
		triangles.add(new Triangle(
				new Vec3f(-1, 0, -1),
				new Vec3f(1, 0, -1),
				new Vec3f(-1, 2, -1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(1, 0, -1),
				new Vec3f(1, 2, -1),
				new Vec3f(-1, 2, -1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));

		// right wall
		triangles.add(new Triangle(
				new Vec3f(1, 0, 1),
				new Vec3f(1, 2, 1),
				new Vec3f(1, 0, -1),
				new Vec3f(0.14f, 0.45f, 0.091f)
		));
		triangles.add(new Triangle(
				new Vec3f(1, 0, -1),
				new Vec3f(1, 2, 1),
				new Vec3f(1, 2, -1),
				new Vec3f(0.14f, 0.45f, 0.091f)
		));

		// left wall
		triangles.add(new Triangle(
				new Vec3f(-1, 0, 1),
				new Vec3f(-1, 2, -1),
				new Vec3f(-1, 2, 1),
				new Vec3f(0.63f, 0.065f, 0.05f)
		));
		triangles.add(new Triangle(
				new Vec3f(-1, 0, 1),
				new Vec3f(-1, 0, -1),
				new Vec3f(-1, 2, -1),
				new Vec3f(0.63f, 0.065f, 0.05f)
		));

		// ceiling
		triangles.add(new Triangle(
				new Vec3f(-1, 2, 1),
				new Vec3f(-1, 2, -1),
				new Vec3f(1, 2, -1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-1, 2, 1),
				new Vec3f(1, 2, -1),
				new Vec3f(1, 2, 1),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));

		// light
		triangles.add(new Triangle(
				new Vec3f(-0.22f, 1.98f, 0.22f),
				new Vec3f(-0.22f, 1.98f, -0.22f),
				new Vec3f(0.22f, 1.98f, -0.22f),
				new Vec3f(0.78f, 0.78f, 0.78f),
				new Vec3f(17f, 12f, 4f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.22f, 1.98f, 0.22f),
				new Vec3f(0.22f, 1.98f, -0.22f),
				new Vec3f(0.22f, 1.98f, 0.22f),
				new Vec3f(0.78f, 0.78f, 0.78f),
				new Vec3f(17f, 12f, 4f)
		));

		// short box
		// top
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.60f, 0.75f),
				new Vec3f(0.70f, 0.60f, 0.17f),
				new Vec3f(0.13f, 0.60f, 0.00f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.60f, 0.75f),
				new Vec3f(0.13f, 0.60f, 0.00f),
				new Vec3f(-0.05f, 0.60f, 0.57f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// left
		triangles.add(new Triangle(
				new Vec3f(-0.05f, 0.00f, 0.57f),
				new Vec3f(-0.05f, 0.60f, 0.57f),
				new Vec3f(0.13f, 0.60f, 0.57f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.05f, 0.00f, 0.57f),
				new Vec3f(0.13f, 0.60f, 0.57f),
				new Vec3f(0.13f, 0.00f, 0.00f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// front
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.00f, 0.75f),
				new Vec3f(0.53f, 0.60f, 0.75f),
				new Vec3f(-0.05f, 0.60f, 0.57f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.00f, 0.75f),
				new Vec3f(-0.05f, 0.60f, 0.57f),
				new Vec3f(-0.05f, 0.00f, 0.57f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// right
		triangles.add(new Triangle(
				new Vec3f(0.70f, 0.00f, 0.17f),
				new Vec3f(0.70f, 0.60f, 0.17f),
				new Vec3f(0.53f, 0.60f, 0.75f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.70f, 0.00f, 0.17f),
				new Vec3f(0.53f, 0.60f, 0.75f),
				new Vec3f(0.53f, 0.00f, 0.75f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// back
		triangles.add(new Triangle(
				new Vec3f(0.13f, 0.00f, 0.00f),
				new Vec3f(0.13f, 0.60f, 0.00f),
				new Vec3f(0.70f, 0.60f, 0.17f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.13f, 0.00f, 0.00f),
				new Vec3f(0.70f, 0.60f, 0.17f),
				new Vec3f(0.70f, 0.00f, 0.17f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// bottom
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.00f, 0.75f),
				new Vec3f(0.70f, 0.00f, 0.17f),
				new Vec3f(0.13f, 0.00f, 0.00f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.53f, 0.00f, 0.75f),
				new Vec3f(-0.05f, 0.00f, 0.57f),
				new Vec3f(0.13f, 0.00f, 0.00f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));

		// tall box
		//top
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 1.20f, 0.09f),
				new Vec3f(0.04f, 1.20f, -0.09f),
				new Vec3f(-0.14f, 1.20f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 1.20f, 0.09f),
				new Vec3f(-0.71f, 1.20f, -0.49f),
				new Vec3f(-0.14f, 1.20f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		//left
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 0.00f, 0.09f),
				new Vec3f(-0.53f, 1.20f, 0.09f),
				new Vec3f(-0.71f, 1.20f, -0.49f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 0.00f, 0.09f),
				new Vec3f(-0.71f, 1.20f, -0.49f),
				new Vec3f(-0.71f, 0.00f, -0.49f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// back
		triangles.add(new Triangle(
				new Vec3f(-0.71f, 0.00f, -0.49f),
				new Vec3f(-0.71f, 1.20f, -0.49f),
				new Vec3f(-0.14f, 1.20f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.71f, 0.00f, -0.49f),
				new Vec3f(-0.14f, 1.20f, -0.67f),
				new Vec3f(-0.14f, 0.00f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// right
		triangles.add(new Triangle(
				new Vec3f(-0.14f, 0.00f, -0.67f),
				new Vec3f(-0.14f, 1.20f, -0.67f),
				new Vec3f(0.04f, 1.20f, -0.09f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.14f, 0.00f, -0.67f),
				new Vec3f(0.04f, 1.20f, -0.09f),
				new Vec3f(0.04f, 0.00f, -0.09f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// front
		triangles.add(new Triangle(
				new Vec3f(0.04f, 0.00f, -0.09f),
				new Vec3f(0.04f, 1.20f, -0.09f),
				new Vec3f(-0.53f, 1.20f, 0.09f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(0.04f, 0.00f, -0.09f),
				new Vec3f(-0.53f, 1.20f, 0.09f),
				new Vec3f(-0.53f, 0.00f, 0.09f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		// bottom
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 0.00f, 0.09f),
				new Vec3f(0.04f, 0.00f, -0.09f),
				new Vec3f(-0.14f, 0.00f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
		triangles.add(new Triangle(
				new Vec3f(-0.53f, 0.00f, 0.09f),
				new Vec3f(-0.71f, 0.00f, -0.49f),
				new Vec3f(-0.14f, 0.00f, -0.67f),
				new Vec3f(0.725f, 0.71f, 0.68f)
		));
	}

}
