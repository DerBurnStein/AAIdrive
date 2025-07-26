"""
simulate_car_data.py
====================

AAIDrive includes various features that depend on live vehicle data
received over the BMW IDrive communication interface. Testing these
features usually requires a physical car, which complicates
development and continuous integration. This script simulates car
sensor data and emits it over different channels, enabling
developers to test and debug AAIDrive without access to a vehicle.

Features
--------

- **Randomized metrics**: The simulator generates plausible values
  for speed (km/h), RPM, fuel level, temperature, and odometer
  reading. Values can be constrained via command‑line arguments.
- **Output channels**:
    * Standard output (default): prints JSON objects each interval.
    * File output: writes JSON lines to a file with ``--logfile``.
    * WebSocket output: if the ``websockets`` library is installed
      and a server URI is provided via ``--websocket``, sends the
      JSON messages over a WebSocket connection.
- **Customisable timing**: Choose the duration of the simulation
  and the interval between messages (e.g., one message every second).
- **Deterministic mode**: When ``--seed`` is provided, the random
  generator produces repeatable sequences useful for reproducible
  tests.

Usage
-----

```
python3 simulate_car_data.py --duration 60 --interval 1 --websocket ws://localhost:8765
```

This will send simulated data every second for one minute to a
WebSocket server running locally. Remove ``--websocket`` to print
data to the console instead.
"""

import argparse
import asyncio
import json
import os
import random
import sys
import time
from dataclasses import dataclass, asdict
from typing import Optional

try:
    import websockets  # type: ignore
    WEBSOCKETS_AVAILABLE = True
except ImportError:
    WEBSOCKETS_AVAILABLE = False


@dataclass
class CarMetrics:
    timestamp: float
    speed_kmh: float
    rpm: int
    fuel_level: float
    coolant_temp_c: float
    odometer_km: float


def generate_metrics(state: dict) -> CarMetrics:
    """Generate a new set of car metrics.

    The state dict retains persistent values like odometer so they
    accumulate realistically between calls.
    """
    # Simulate speed between 0 and 160 km/h with small variance
    speed = max(0.0, min(160.0, state.get("speed", 0.0) + random.uniform(-5, 5)))
    state["speed"] = speed

    # RPM roughly correlates with speed but can fluctuate
    rpm = int(max(700, min(6000, speed * 40 + random.uniform(-200, 200))))

    # Fuel level slowly decreases over time
    fuel = max(0.0, state.get("fuel", 100.0) - speed / 1000 - random.uniform(0, 0.05))
    state["fuel"] = fuel

    # Coolant temperature stabilizes around 90°C with minor changes
    temp = max(70.0, min(110.0, state.get("temp", 90.0) + random.uniform(-0.5, 0.5)))
    state["temp"] = temp

    # Odometer increases with distance traveled per interval (speed * time)
    # Assume interval is one second for odometer increment; will scale later
    odometer = state.get("odometer", 0.0) + speed / 3600.0  # km per second
    state["odometer"] = odometer

    return CarMetrics(
        timestamp=time.time(),
        speed_kmh=round(speed, 2),
        rpm=rpm,
        fuel_level=round(fuel, 2),
        coolant_temp_c=round(temp, 1),
        odometer_km=round(odometer, 3),
    )


async def websocket_sender(uri: str, queue: asyncio.Queue) -> None:
    """Send JSON messages from the queue over a WebSocket connection."""
    async with websockets.connect(uri) as websocket:
        while True:
            message = await queue.get()
            await websocket.send(message)
            queue.task_done()


async def simulate(duration: float, interval: float, log_file: Optional[str], websocket_uri: Optional[str], seed: Optional[int]) -> None:
    """Run the simulation for the given duration and interval."""
    if seed is not None:
        random.seed(seed)

    state = {}
    start_time = time.time()
    end_time = start_time + duration if duration > 0 else float("inf")

    queue: Optional[asyncio.Queue] = None
    ws_task = None
    # Setup websocket sender if requested and available
    if websocket_uri:
        if not WEBSOCKETS_AVAILABLE:
            print("WebSocket output requested but the 'websockets' library is not installed.", file=sys.stderr)
            return
        queue = asyncio.Queue()
        ws_task = asyncio.create_task(websocket_sender(websocket_uri, queue))

    # Setup file logging
    log_fp = open(log_file, "a") if log_file else None

    try:
        while time.time() < end_time:
            metrics = generate_metrics(state)
            json_str = json.dumps(asdict(metrics))
            # Write to chosen outputs
            if websocket_uri and queue:
                await queue.put(json_str)
            else:
                print(json_str)
            if log_fp:
                log_fp.write(json_str + "\n")
                log_fp.flush()
            await asyncio.sleep(interval)
    finally:
        if ws_task:
            await queue.join()  # Wait until all messages are sent
            ws_task.cancel()
            try:
                await ws_task
            except asyncio.CancelledError:
                pass
        if log_fp:
            log_fp.close()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Simulate vehicle sensor data for AAIDrive")
    parser.add_argument(
        "--duration",
        type=float,
        default=30.0,
        help="Total duration of the simulation in seconds (0 for infinite)",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=1.0,
        help="Interval between data messages in seconds",
    )
    parser.add_argument(
        "--logfile",
        help="Path to a file where JSON messages will be appended",
    )
    parser.add_argument(
        "--websocket",
        dest="websocket_uri",
        help="WebSocket URI to send messages to (e.g., ws://localhost:8765)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        help="Seed for the random number generator to make output deterministic",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    try:
        asyncio.run(
            simulate(
                duration=args.duration,
                interval=args.interval,
                log_file=args.logfile,
                websocket_uri=args.websocket_uri,
                seed=args.seed,
            )
        )
    except KeyboardInterrupt:
        print("\nSimulation interrupted by user.")


if __name__ == "__main__":
    main()
