package com.gtnewhorizon.structurelib.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.gtnewhorizon.structurelib.util.PositionedRect;

public class ProjectionUtils {

    // TODO: Replace this with something from JOML; and/or something that allocates less
    public static Vector3f unProject(PositionedRect rect, Vector3f eyePos, Vector3f lookat, int mouseX, int mouseY) {
        int width = rect.getWidth();
        int height = rect.getHeight();

        double aspectRatio = ((double) width / (double) height);
        double fov = ((60 / 2d)) * (Math.PI / 180);

        double a = -((double) (mouseX - rect.x) / (double) width - 0.5) * 2;
        double b = -((double) (height - (mouseY - rect.y)) / (double) height - 0.5) * 2;
        double tanf = Math.tan(fov);

        Vector3f lookVec = new Vector3f();
        eyePos.sub(lookat, lookVec);
        float yawn = (float) Math.atan2(lookVec.x, -lookVec.z);
        float pitch = (float) Math.atan2(lookVec.y, Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z));

        Matrix4f rot = new Matrix4f();
        rot.rotate(yawn, new Vector3f(0, -1, 0));
        rot.rotate(pitch, new Vector3f(1, 0, 0));
        Vector4f forward = new Vector4f(0, 0, 1, 0);
        Vector4f up = new Vector4f(0, 1, 0, 0);
        Vector4f left = new Vector4f(1, 0, 0, 0);

        rot.transform(forward, forward);
        rot.transform(up, up);
        rot.transform(left, left);

        Vector3f result = new Vector3f(forward.x, forward.y, forward.z);
        result.add(
                (float) (left.x * tanf * aspectRatio * a),
                (float) (left.y * tanf * aspectRatio * a),
                (float) (left.z * tanf * aspectRatio * a));
        result.add((float) (up.x * tanf * b), (float) (up.y * tanf * b), (float) (up.z * tanf * b));
        return result.normalize();
    }
}
