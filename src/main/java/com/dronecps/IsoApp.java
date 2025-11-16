package com.dronecps;

import com.dronecps.core.sr.Scheduler;
import com.dronecps.core.sr.SrcComponent;
import com.dronecps.core.util.Blackboard;
import com.dronecps.core.util.Vec2;
import com.dronecps.types.Pose;
import com.dronecps.bus.CommBus;
import com.dronecps.env.EnvironmentSim;
import com.dronecps.sensors.*;
import com.dronecps.agents.drone.*;
import com.dronecps.cyber.*;
import com.dronecps.ui3d.IsoPanel3D;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

public final class IsoApp {
    private static final int  NUM_DRONES = 3;
    private static final long TICK_MS    = 50L;

    public static void main(String[] args) {
        Blackboard bb = new Blackboard();
        Scheduler  sched = new Scheduler();
        EnvironmentSim env = new EnvironmentSim(bb, NUM_DRONES);
        System.out.println("Scenario: " + env.scenario());

        // Sensors per drone
        var cams = new ArrayList<CameraSensor>();
        var gps  = new ArrayList<GpsSensor>();
        var heat = new ArrayList<HeatSensor>();
        for (int i=0;i<NUM_DRONES;i++){
            cams.add(new CameraSensor(bb,i));
            gps.add(new GpsSensor(bb,i));
            heat.add(new HeatSensor(bb,i));
        }
        // Wrap sensors as SRCs (emit during COMMIT)
        class SensorWrap implements SrcComponent{
            Runnable r; String n; SensorWrap(String n,Runnable r){ this.n=n; this.r=r; }
            public void sample(long t){} public void compute(long t){} public void commit(long t){ r.run(); }
            public String name(){ return n; }
        }
        for(int i=0;i<NUM_DRONES;i++){
            final int id=i;
            sched.add(new SensorWrap("Cam#"+id,  ()-> cams.get(id).emit()));
            sched.add(new SensorWrap("GPS#"+id,  ()-> gps.get(id).emit()));
            sched.add(new SensorWrap("Heat#"+id, ()-> heat.get(id).emit()));
        }

        // Drone SRCs
        var cols=new ArrayList<SensorCollector>(); var ests=new ArrayList<PoseEstimator>();
        var dets=new ArrayList<Perception>(); var cas=new ArrayList<CollisionAvoidance>();
        var foll=new ArrayList<CommandFollower>(); var acts=new ArrayList<ActuatorController>();
        for(int i=0;i<NUM_DRONES;i++){
            cols.add(new SensorCollector(bb,i));
            ests.add(new PoseEstimator(bb,i));
            dets.add(new Perception(bb,i));
            cas.add(new CollisionAvoidance(bb,i));
            foll.add(new CommandFollower(bb,i));
            acts.add(new ActuatorController(bb,i));
        }
        cols.forEach(sched::add); ests.forEach(sched::add); dets.forEach(sched::add);
        cas.forEach(sched::add);  foll.forEach(sched::add); acts.forEach(sched::add);

        // Cyber + Bus
        var cov=new CoverageMonitor(bb,50,50,2.0);
        var ta =new TaskAllocator(bb,NUM_DRONES);
        var pp =new PathPlanner(bb);
        var cmd=new CommandDispatcher(bb);
        var bus=new CommBus(bb);
        sched.add(cov); sched.add(ta); sched.add(pp); sched.add(cmd);
        sched.add(new SensorWrap("CommBus", bus::deliver));

        // --- UI (3D isometric) ---
        IsoPanel3D panel = new IsoPanel3D(bb, NUM_DRONES);
        JFrame frame = new JFrame("Drone CPS — Isometric 3D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);

        // Key bindings
        var im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var am = panel.getActionMap();
        im.put(KeyStroke.getKeyStroke("SPACE"), "toggle");
        am.put("toggle", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.togglePause(); }});
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        am.put("quit", new AbstractAction(){ public void actionPerformed(ActionEvent e){ System.exit(0);} });
        im.put(KeyStroke.getKeyStroke("A"), "rotL");
        am.put("rotL", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.rotateYaw(-5); }});
        im.put(KeyStroke.getKeyStroke("D"), "rotR");
        am.put("rotR", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.rotateYaw(5); }});
        im.put(KeyStroke.getKeyStroke("W"), "zoomIn");
        am.put("zoomIn", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.zoom(1.1); }});
        im.put(KeyStroke.getKeyStroke("S"), "zoomOut");
        am.put("zoomOut", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.zoom(0.9); }});
        // --- Movement bindings (camera panning in world space, aligned to yaw) ---
        final double STEP = 2.0;            // world units per key press
        final double FAST = STEP * 3.0;     // with Shift

// Forward / Back
        im.put(KeyStroke.getKeyStroke("UP"), "cam_fwd");
        am.put("cam_fwd", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveForward(STEP); }});
        im.put(KeyStroke.getKeyStroke("DOWN"), "cam_back");
        am.put("cam_back", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveForward(-STEP); }});

// Strafe Left / Right
        im.put(KeyStroke.getKeyStroke("LEFT"), "cam_left");
        am.put("cam_left", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveRight(-STEP); }});
        im.put(KeyStroke.getKeyStroke("RIGHT"), "cam_right");
        am.put("cam_right", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveRight( STEP); }});

// Faster with Shift
        im.put(KeyStroke.getKeyStroke("shift UP"), "cam_fwd_fast");
        am.put("cam_fwd_fast", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveForward(FAST); }});
        im.put(KeyStroke.getKeyStroke("shift DOWN"), "cam_back_fast");
        am.put("cam_back_fast", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveForward(-FAST); }});
        im.put(KeyStroke.getKeyStroke("shift LEFT"), "cam_left_fast");
        am.put("cam_left_fast", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveRight(-FAST); }});
        im.put(KeyStroke.getKeyStroke("shift RIGHT"), "cam_right_fast");
        am.put("cam_right_fast", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.moveRight( FAST); }});

// Reset camera
        im.put(KeyStroke.getKeyStroke("C"), "cam_reset");
        am.put("cam_reset", new AbstractAction(){ public void actionPerformed(ActionEvent e){ panel.resetCamera(); }});

        // Main loop (20 Hz) using Swing timer (explicit class to avoid util.Timer ambiguity)
        javax.swing.Timer timer = new javax.swing.Timer((int)TICK_MS, e -> {
            if (panel.isPaused()) return;

            // 1) Plant integration using last velocities
            Map<Integer, Vec2> vels = new HashMap<>();
            for (int i=0;i<NUM_DRONES;i++){
                Vec2 v = bb.get(CommandFollower.velKey(i), Vec2.class);
                if (v!=null) vels.put(i, v);
            }
            env.step(vels, TICK_MS/1000.0);

            // 2) SR major cycle + commit
            sched.tick(System.currentTimeMillis());
            bb.commit();

            // 3) Handle actions (log when done)
            String scen = bb.get(EnvironmentSim.KEY_SCENARIO, String.class);
            Vec2[] dronePos = new Vec2[NUM_DRONES];
            for (int i=0;i<NUM_DRONES;i++){
                Pose p = bb.get(EnvironmentSim.KEY_POSE_PREFIX + i, Pose.class);
                if (p!=null) dronePos[i] = p.pos();
            }
            if ("FLOOD".equals(scen)) {
                for (int vi=0; vi<env.victims().size(); vi++){
                    var v = env.victims().get(vi);
                    if (v.served) continue;
                    for (int i=0;i<NUM_DRONES;i++){
                        Vec2 dp = dronePos[i]; if (dp==null) continue;
                        if (dp.sub(v.pos).norm() < 1.5){
                            v.served = true;
                            System.out.printf("FLOOD: Drone %d delivered supplies to victim %d at (%.1f, %.1f)%n",
                                    i, vi, v.pos.x(), v.pos.y());
                            break;
                        }
                    }
                }
            } else if ("FIRE".equals(scen)) {
                for (int hi=0; hi<env.hotspots().size(); hi++){
                    var h = env.hotspots().get(hi);
                    if (!h.alive) continue;
                    for (int i=0;i<NUM_DRONES;i++){
                        Vec2 dp = dronePos[i]; if (dp==null) continue;
                        if (dp.sub(h.c).norm() < h.r + 1.0){
                            h.alive = false;
                            System.out.printf("FIRE: Drone %d extinguished hotspot %d at (%.1f, %.1f)%n",
                                    i, hi, h.c.x(), h.c.y());
                            break;
                        }
                    }
                }
            }

            // 4) Repaint 3D view
            panel.repaint();
        });
        timer.start();
    }
}
