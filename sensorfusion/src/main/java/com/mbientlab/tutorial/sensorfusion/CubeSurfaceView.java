/*
 * Code taken from http://www.edumobile.org/android/touch-rotate-example-in-android/
 */
package com.mbientlab.tutorial.sensorfusion;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.mbientlab.metawear.module.SensorFusion.Quaternion;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by etsai on 11/23/16.
 */
public class CubeSurfaceView extends GLSurfaceView {
    public CubeSurfaceView(Context context) {
        super(context);
        mRenderer = new CubeRenderer();
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void updateRotation(Quaternion value) {
        mRenderer.quaterion = value;
    }

    private class CubeRenderer implements GLSurfaceView.Renderer {
        public CubeRenderer() {
            mCube = new Cube();
        }

        public void onDrawFrame(GL10 gl) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3.0f);

            // Convert quaterion values to glRotatef compatible values
            // http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-17-quaternions/
            float halfAngle = (float) Math.acos(quaterion.w);
            float halfSin = (float) Math.sin(halfAngle);
            gl.glRotatef((float) (halfAngle * 360f / Math.PI), quaterion.x / halfSin, quaterion.y / halfSin, quaterion.z / halfSin);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            mCube.draw(gl);
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);

            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glDisable(GL10.GL_DITHER);
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                    GL10.GL_FASTEST);

            gl.glClearColor(0,0,0,0);
            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glEnable(GL10.GL_DEPTH_TEST);
        }
        private Cube mCube;
        public Quaternion quaterion = new Quaternion(0, 1f, 0f, 0f);
    }

    private CubeRenderer mRenderer;
}
