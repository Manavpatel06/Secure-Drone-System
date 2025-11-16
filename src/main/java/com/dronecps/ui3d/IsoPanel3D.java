package com.dronecps.ui3d;

import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.types.Pose;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Lightweight isometric 3D renderer with yaw/zoom + camera panning. */
public class IsoPanel3D extends JPanel {
    private final Blackboard bb;
    private final int numDrones;

    // Camera
    private double yawDeg = 45.0;     // rotation around Z
    private double scale  = 10.0;     // pixels per world unit
    private double camX   = 10.0;     // camera center (world X)  (starts at base)
    private double camY   = 10.0;     // camera center (world Y)
    private final int margin = 16;
    private boolean paused = false;

    // Drone altitude (visual only)
    private final double droneZ = 3.5;

    public IsoPanel3D(Blackboard bb, int numDrones) {
        this.bb = bb;
        this.numDrones = numDrones;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 680));
    }

    // ---- Controls API ----
    public void togglePause(){ paused = !paused; }
    public boolean isPaused(){ return paused; }
    public void rotateYaw(double deltaDeg){ yawDeg += deltaDeg; repaint(); }

    public void zoom(double factor){
        scale *= factor;
        if (scale < 4)  scale = 4;
        if (scale > 40) scale = 40;
        repaint();
    }

    /** Move forward/back along current yaw (dist in world units). */
    public void moveForward(double dist){
        double a = Math.toRadians(yawDeg);
        camX += Math.cos(a) * dist;
        camY += Math.sin(a) * dist;
        repaint();
    }

    /** Strafe right/left relative to current yaw (dist in world units). */
    public void moveRight(double dist){
        double a = Math.toRadians(yawDeg + 90.0);
        camX += Math.cos(a) * dist;
        camY += Math.sin(a) * dist;
        repaint();
    }

    /** Reset camera to base and defaults. */
    public void resetCamera(){
        camX = 10.0; camY = 10.0; yawDeg = 45.0; scale = 10.0;
        repaint();
    }

    // ---- Projection ----
    // Translate world by (-camX, -camY), rotate by yaw, then isometric project.
    private Point project(double x, double y, double z) {
        // camera-space translation
        double xt = x - camX, yt = y - camY;

        // rotate around Z by yaw
        double a = Math.toRadians(yawDeg);
        double cx = Math.cos(a), sx = Math.sin(a);
        double xr =  cx * xt - sx * yt;
        double yr =  sx * xt + cx * yt;

        // classic iso: Xs = (xr - yr), Ys = (xr + yr)/2 - z
        double xs = (xr - yr) * scale;
        double ys = ((xr + yr) * 0.5 - z) * scale;

        int cxp = getWidth()  / 2;
        int cyp = getHeight() - margin - 40; // anchor near bottom
        return new Point((int)Math.round(cxp + xs), (int)Math.round(cyp - ys));
    }

    private void drawIsoLine(Graphics2D g2, double x1, double y1, double z1, double x2, double y2, double z2){
        Point p1 = project(x1,y1,z1), p2 = project(x2,y2,z2);
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Border & grid
        g2.setColor(new Color(232,232,232));
        g2.fillRect(margin, margin, w-2*margin, h-2*margin);
        g2.setColor(new Color(220,220,220));
        g2.setStroke(new BasicStroke(1f));
        for (int x=0; x<=80; x+=10) drawIsoLine(g2, x, 0, 0, x, 60, 0);
        for (int y=0; y<=60; y+=10) drawIsoLine(g2, 0, y, 0, 80, y, 0);

        // Obstacles as pillars
        g2.setColor(new Color(130,130,130));
        @SuppressWarnings("unchecked")
        List<EnvironmentSim.Circle> obs = (List<EnvironmentSim.Circle>) bb.get(EnvironmentSim.KEY_OBS, List.class);
        if (obs != null) {
            for (var c : obs) {
                Point base = project(c.c().x(), c.c().y(), 0);
                Point top  = project(c.c().x(), c.c().y(), c.r());
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(base.x, base.y, top.x, top.y);
            }
        }

        String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);

        // FLOOD victims
        if ("FLOOD".equals(scen)) {
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Victim> vs = (List<EnvironmentSim.Victim>) bb.get(EnvironmentSim.KEY_VICTIMS, List.class);
            if (vs != null) for (var v : vs) {
                Point p = project(v.pos.x(), v.pos.y(), 0);
                g2.setColor(v.served ? new Color(40,160,60) : new Color(30,120,210));
                int r = 7; g2.fillOval(p.x-r, p.y-r, 2*r, 2*r);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(v.served ? "✓" : "SOS", p.x+10, p.y-8);
            }
        }

        // FIRE hotspots
        if ("FIRE".equals(scen)) {
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Hotspot> hs = (List<EnvironmentSim.Hotspot>) bb.get(EnvironmentSim.KEY_HOTSPOTS, List.class);
            if (hs != null) for (var hsp : hs) {
                Point base = project(hsp.c.x(), hsp.c.y(), 0);
                Point top  = project(hsp.c.x(), hsp.c.y(), hsp.r);
                g2.setColor(hsp.alive ? new Color(220,60,40,180) : new Color(120,120,120,140));
                g2.setStroke(new BasicStroke(6f));
                g2.drawLine(base.x, base.y, top.x, top.y);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(hsp.alive ? "FIRE" : "OUT", top.x+8, top.y-8);
            }
        }

        // Drones (at altitude)
        for (int i=0; i<numDrones; i++){
            Pose pose = bb.get(EnvironmentSim.KEY_POSE_PREFIX + i, Pose.class);
            if (pose == null) continue;
            Point pd = project(pose.pos().x(), pose.pos().y(), droneZ);
            g2.setColor(new Color(40,120,220));
            g2.fillOval(pd.x-6, pd.y-6, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString("D"+i, pd.x+10, pd.y-8);

            // shadow on ground
            Point sh = project(pose.pos().x(), pose.pos().y(), 0);
            g2.setColor(new Color(0,0,0,50));
            g2.fillOval(sh.x-4, sh.y-2, 8, 4);
        }

        // HUD
        g2.setColor(Color.DARK_GRAY);
        String hud = "Camera @ (%.1f, %.1f)  yaw=%.0f°  Scenario: %s   [SPACE pause, ESC quit, A/D rotate, W/S zoom, Arrows move, Shift=faster, C reset]"
                .formatted(camX, camY, yawDeg, scen==null?"—":scen);
        g2.drawString(hud, margin+6, margin+14);

        if (paused) {
            g2.setColor(new Color(0,0,0,110));
            g2.fillRect(0,0,w,h);
            g2.setColor(Color.WHITE);
            g2.drawString("PAUSED", w/2-20, h/2);
        }
    }
}
