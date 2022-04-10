package at.gitterleem.application.core.math;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Vec3f {

	public float x = 0f;
	public float y = 0f;
	public float z = 0f;

	public Vec3f(Vec3f v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public Vec3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3f add(Vec3f v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}

	public Vec3f set(Vec3f v) {
		x = v.x;
		y = v.y;
		z = v.z;
		return this;
	}

	public static Vec3f add(Vec3f v1, Vec3f v2) {
		Vec3f v = new Vec3f(v1);
		return v.add(v2);
	}

	public Vec3f sub(Vec3f v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}

	public static Vec3f sub(Vec3f v1, Vec3f v2) {
		Vec3f v = new Vec3f(v1);
		return v.sub(v2);
	}

	public Vec3f mul(Vec3f v) {
		x *= v.x;
		y *= v.y;
		z *= v.z;
		return this;
	}

	public Vec3f mul(float s) {
		x *= s;
		y *= s;
		z *= s;
		return this;
	}

	public static Vec3f mul(Vec3f v1, Vec3f v2) {
		Vec3f v = new Vec3f(v1);
		return v.mul(v2);
	}

	public static Vec3f mul(Vec3f v1, float s) {
		Vec3f v = new Vec3f(v1);
		return v.mul(s);
	}

	public Vec3f div(Vec3f v) {
		x /= v.x;
		y /= v.y;
		z /= v.z;
		return this;
	}

	public Vec3f div(float s) {
		float d = 1f / s;
		return mul(d);
	}

	public static Vec3f div(Vec3f v1, Vec3f v2) {
		Vec3f v = new Vec3f(v1);
		return v.div(v2);
	}

	public static Vec3f div(Vec3f v1, float s) {
		Vec3f v = new Vec3f(v1);
		return v.div(s);
	}

	public Vec3f clamp(float min, float max) {
		x = x < min ? min : Math.min(x, max);
		y = y < min ? min : Math.min(y, max);
		z = z < min ? min : Math.min(z, max);
		return this;
	}

	public float dot(Vec3f v) {
		return x * v.x + y * v.y + z * v.z;
	}

	public Vec3f cross(Vec3f v) {
		float s1 = y * v.z - z * v.y;
		float s2 = z * v.x - x * v.z;
		float s3 = x * v.y - y * v.x;
		x = s1;
		y = s2;
		z = s3;
		return this;
	}

	public static Vec3f cross(Vec3f v1, Vec3f v2) {
		Vec3f v = new Vec3f(v1);
		return v.cross(v2);
	}

	public float lengthSquared() {
		return x * x + y * y + z * z;
	}

	public float length() {
		return (float) Math.sqrt(lengthSquared());
	}

	public Vec3f normalize() {
		return div(length());
	}

	public float max() {
		return Math.max(x, Math.max(y, z));
	}

	public float min() {
		return Math.min(x, Math.min(y, z));
	}

	public float angle(Vec3f v) {
		return (float) Math.acos(dot(v) / (length() + v.length()));
	}

	public static float angle(Vec3f v1, Vec3f v2) {
		return v1.angle(v2);
	}

	@Override
	public String toString() {
		return String.format("(%.3f, %.3f, %.3f)", x, y, z);
	}
}
