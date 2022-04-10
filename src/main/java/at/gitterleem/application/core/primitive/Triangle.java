package at.gitterleem.application.core.primitive;

import at.gitterleem.application.core.math.Vec3f;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Random;

@NoArgsConstructor
public class Triangle {

	public Vec3f v1;
	public Vec3f v2;
	public Vec3f v3;

	private Vec3f normal = null;

	@Getter @Setter
	private Vec3f color = null;
	@Getter @Setter
	private Vec3f emission = null;

	public Triangle(Vec3f v1, Vec3f v2, Vec3f v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	public Triangle(Vec3f v1, Vec3f v2, Vec3f v3, Vec3f color) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.color = color;
	}

	public Triangle(Vec3f v1, Vec3f v2, Vec3f v3, Vec3f color, Vec3f emission) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.color = color;
		this.emission = emission;
	}

	public Vec3f getNormal() {
		if(normal != null) {
			return normal;
		}

		normal = Vec3f.cross(Vec3f.sub(v2, v1), Vec3f.sub(v3, v1));
		return normal.normalize();
	}

	public boolean intersect(Vec3f rayOrigin, Vec3f rayDirection, Vec3f outIntersectPoint) {
		float epsilon = 0.00001f;

		Vec3f edge1 = Vec3f.sub(v2, v1);
		Vec3f edge2 = Vec3f.sub(v3, v1);

		// check if ray and triangle is parallel
		Vec3f h = Vec3f.cross(rayDirection, edge2);
		float a = edge1.dot(h);
		if(Math.abs(a) < epsilon) {
			return false;
		}

		// check if intersect is within triangle
		float f = 1f / a;
		Vec3f s = Vec3f.sub(rayOrigin, v1);
		float u = f * s.dot(h);
		if(u < 0f || u > 1f) {
			return false;
		}

		Vec3f q = Vec3f.cross(s, edge1);
		float v = f * rayDirection.dot(q);
		if(v < 0f || u + v > 1f) {
			return false;
		}

		// get distance t from ray origin to intersect
		float t = f * edge2.dot(q);

		if(t > epsilon) {
			outIntersectPoint.set(Vec3f.add(rayOrigin, Vec3f.mul(rayDirection, t)));
			return true;
		}

		return false;
	}

	// TODO: check if this is uniform
	// https://stackoverflow.com/questions/19654251/random-point-inside-triangle-inside-java
	public Vec3f randomSamplePoint(Random random) {
			float r1 = random.nextFloat();
			float r2 = random.nextFloat();

			if(r1 + r2 > 1f) {
				r1 = 1f - r1;
				r2 = 1f - r2;
			}

			float a = 1f - r1 - r2;

			return Vec3f.mul(v1, a).add(Vec3f.mul(v2, r1)).add(Vec3f.mul(v3, r2));
	}

}
