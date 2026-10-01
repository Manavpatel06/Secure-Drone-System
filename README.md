# Secure Drone System — Drone Swarm CPS Simulator

A Java simulation of a **cyber-physical system (CPS)**: a small swarm of drones coordinated by a ground-control layer to respond to a disaster. Each run randomly generates one of two missions:

| Scenario | Targets | Drone goal |
|---|---|---|
| **FIRE** | 2–4 heat hotspots | Fly to each hotspot and extinguish it |
| **FLOOD** | 3–5 stranded victims | Fly to each victim and deliver supplies |

Drones launch from a shared base station on an 80×60 map with static obstacles and a constant wind, and the simulation runs at **20 Hz** with a live 2D or isometric 3D view.

---

## Features

- **Synchronous reactive (SR) execution model** — every component runs a `sample → compute → commit` cycle, so results don't depend on component ordering.
- **Double-buffered blackboard** — components communicate only through shared keys; writes become visible on the next tick.
- **Per-drone onboard pipeline** — sensors → sensor fusion → pose estimation → perception → command following → actuation.
- **Ground-control ("cyber") layer** — coverage monitoring, greedy nearest-drone task allocation, path planning and command dispatch.
- **Simulated comm bus** that routes commands to individual drones.
- **Two visualizations** — top-down 2D (`App`) and rotatable isometric 3D (`IsoApp`).

---

## Architecture

```
┌──────────────────────────── one tick (50 ms) ─────────────────────────────┐
│                                                                           │
│  EnvironmentSim (physics, wind, targets)                                  │
│        │ true poses                                                       │
│        ▼                                                                  │
│  Sensors: GPS (+noise) · Camera · Heat                                    │
│        ▼                                                                  │
│  SensorCollector → PoseEstimator → Perception             [per drone]     │
│        │ estimated poses                                                  │
│        ▼                                                                  │
│  CoverageMonitor · TaskAllocator → PathPlanner → CommandDispatcher        │
│        │                                                  [ground control]│
│        ▼                                                                  │
│  CommBus (routes commands by drone ID)                                    │
│        ▼                                                                  │
│  CollisionAvoidance → CommandFollower → ActuatorController [per drone]    │
│        │ velocity commands                                                │
│        └──────────────► back into EnvironmentSim on the next tick         │
└───────────────────────────────────────────────────────────────────────────┘
```

Each tick, the main loop:
1. Steps the physics using the latest velocity commands.
2. Runs the scheduler (all `sample`, then all `compute`, then all `commit`).
3. Commits the blackboard (next state → current state).
4. Checks whether drones have reached targets and logs the results.
5. Repaints the UI.

### Task allocation

`TaskAllocator` collects active targets (alive hotspots or unserved victims), then greedily assigns each target to the **nearest unassigned drone**. Drones left without a target are sent to a random patrol waypoint.

---

## Project structure

```
src/main/java/com/dronecps/
├── App.java                 # Entry point — 2D view
├── IsoApp.java              # Entry point — isometric 3D view
├── core/
│   ├── sr/                  # Scheduler + SrcComponent (sample/compute/commit)
│   └── util/                # Blackboard, Vec2
├── env/EnvironmentSim.java  # World model: physics, wind, obstacles, scenarios
├── sensors/                 # GpsSensor, CameraSensor, HeatSensor
├── agents/drone/            # Onboard pipeline components
├── cyber/                   # CoverageMonitor, TaskAllocator, PathPlanner, CommandDispatcher
├── bus/CommBus.java         # Command routing to drones
├── types/                   # Records: Pose, Cmd, Path, Detection, Cell
├── ui/SimPanel.java         # 2D renderer
└── ui3d/IsoPanel3D.java     # Isometric 3D renderer
```

---

## Getting started

**Requirements:** JDK 17+

### From the command line

```bash
git clone https://github.com/Manavpatel06/secure-drone-system.git
cd secure-drone-system
javac -d build $(find src -name "*.java")

java -cp build com.dronecps.App      # 2D view
java -cp build com.dronecps.IsoApp   # isometric 3D view
```

### From IntelliJ IDEA

Open the project folder, set the project SDK to Java 17, and run `App` or `IsoApp`.

### Controls

| Key | Action | View |
|---|---|---|
| `Space` | Pause / resume | Both |
| `Esc` | Quit | Both |
| `A` / `D` | Rotate camera | 3D |
| `W` / `S` | Zoom in / out | 3D |

The chosen scenario and mission events print to the console, for example:

```
Scenario = FIRE  (seed=...)
FIRE: Drone 1 extinguished hotspot 0 at (42.3, 18.7)
```

---

## Roadmap

Current status is **V1** (core simulation). Planned work:

- [ ] **Secure communications** — authenticate and integrity-check commands on `CommBus` (e.g. HMAC signatures, nonces for replay protection).
- [ ] **Attack simulation & detection** — GPS spoofing, command injection, and message tampering, with detectors on the drone side.
- [ ] **Collision avoidance** — steer around obstacles and other drones (currently a pass-through).
- [ ] **Path planning** — multi-waypoint routes around obstacles (currently a pass-through).
- [ ] **State estimation** — filter noisy GPS (e.g. Kalman filter) instead of using it raw.
- [ ] Build tooling (Maven/Gradle), unit tests, and a shared base for `App` / `IsoApp`.

---

## Tech stack

->Java 17 
->Swing / Java2D 
->Blackboard
