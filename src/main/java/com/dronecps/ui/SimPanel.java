package com.dronecps.ui;

import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.types.Pose;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Simple visualization of drones, obstacles, victims (flood) and hotspots (fire). */
public class SimPanel extends JPanel {
    private final Blackboard bb;
    private final int numDrones;
    private final double scale;
    private final int margin = 16;
    private volatile boolean paused = false;

    public SimPanel(Blackboard bb, int numDrones) {
        this(bb, numDrones, 10.0);
    }

    public SimPanel(Blackboard bb, int numDrones, double scale) {
        this.bb = bb; this.numDrones = numDrones; this.scale = scale;
        setPreferredSize(new Dimension(900, 600));
        setBackground(Color.WHITE);
    }

    public boolean isPaused(){ return paused; }
    public void pause(){ paused=true; }
    public void resume(){ paused=false; }

    private Point toScreen(Vec2 p) {
        int x = margin + (int) Math.round(p.x() * scale);
        int y = margin + (int) Math.round(p.y() * scale);
        return new Point(x, y);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Border and grid
        g2.setColor(new Color(235,235,235));
        g2.fillRect(margin, margin, w-2*margin, h-2*margin);
        g2.setColor(new Color(215,215,215));
        int stepPx = (int) Math.round(10 * scale);
        for (int x=margin; x<w-margin; x+=stepPx) g2.drawLine(x, margin, x, h-margin);
        for (int y=margin; y<h-margin; y+=stepPx) g2.drawLine(margin, y, w-margin, y);

        // Obstacles
        g2.setColor(new Color(120,120,120));
        @SuppressWarnings("unchecked")
        List<EnvironmentSim.Circle> obs = (List<EnvironmentSim.Circle>) bb.get(EnvironmentSim.KEY_OBS, List.class);
        if (obs != null) for (var c : obs) {
            Point pc = toScreen(c.c()); int r = (int) Math.round(c.r() * scale);
            g2.fillOval(pc.x - r, pc.y - r, 2*r, 2*r);
        }

        String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);
        if ("FLOOD".equals(scen)) {
            // Victims
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Victim> vs = (List<EnvironmentSim.Victim>) bb.get(EnvironmentSim.KEY_VICTIMS, List.class);
            if (vs != null) for (var v : vs) {
                Point pv = toScreen(v.pos);
                g2.setColor(v.served ? new Color(40,160,60) : new Color(20,120,200));
                int r=7; g2.fillOval(pv.x-r, pv.y-r, 2*r, 2*r);
                g2.setColor(Color.BLACK); g2.drawString(v.served ? "✓" : "SOS", pv.x+8, pv.y-8);
            }
        } else if ("FIRE".equals(scen)) {
            // Hotspots
            @SuppressWarnings("unchecked")
            List<EnvironmentSim.Hotspot> hs = (List<EnvironmentSim.Hotspot>) bb.get(EnvironmentSim.KEY_HOTSPOTS, List.class);
            if (hs != null) for (var hsp : hs) {
                Point pc = toScreen(hsp.c);
                int r = (int) Math.round(hsp.r * scale);
                g2.setColor(hsp.alive ? new Color(220,60,40,180) : new Color(160,160,160,120));
                g2.fillOval(pc.x - r, pc.y - r, 2*r, 2*r);
                g2.setColor(Color.DARK_GRAY); g2.drawString(hsp.alive ? "FIRE" : "OUT", pc.x+8, pc.y-8);
            }
        }

        // Drones
        for (int i=0;i<numDrones;i++){
            Pose p = bb.get(EnvironmentSim.KEY_POSE_PREFIX + i, Pose.class);
            if (p==null) continue;
            Point pd = toScreen(p.pos());
            g2.setColor(new Color(40,120,220));
            g2.fillOval(pd.x-6, pd.y-6, 12, 12);
            g2.setColor(Color.BLACK); g2.drawString("D"+i, pd.x+8, pd.y-8);
        }

        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Scenario: " + (scen==null?"—":scen) + "   (SPACE: pause/resume, ESC: quit)", margin+6, margin+14);

        if (paused) {
            g2.setColor(new Color(0,0,0,110));
            g2.fillRect(0,0,w,h);
            g2.setColor(Color.WHITE);
            g2.drawString("PAUSED", w/2-20, h/2);
        }
    }
}
