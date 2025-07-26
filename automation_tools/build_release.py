#!/usr/bin/env python3
"""
Build Release Script for AAIdrive
================================

This script automates some parts of the Android build pipeline for AAIdrive.
It wraps Gradle tasks to assemble different build variants (for example the
`sentry` and `nonalytics` flavors) and optionally cleans the build
environment beforehand. The script does not require any third‑party
dependencies, but it assumes that the Android build tools and Gradle
wrapper (gradlew) are available in the repository root.

Usage:
    python build_release.py [--clean] [--flavor FLAVOR]

Options:
    --clean          Perform a clean build by running `./gradlew clean` before
                     assembling the APKs.
    --flavor FLAVOR  Specify a build flavor: "sentry", "nonalytics", or
                     "all" (default). When "all" is selected, both release
                     variants will be assembled.

Example:
    python build_release.py --clean --flavor sentry

Note: This script is intended to be run by developers on their local
machines or in a CI environment. It uses subprocess to invoke Gradle
commands and does not handle signing or uploading of the generated APKs.
Those steps should be performed separately using appropriate credentials.
"""

import argparse
import os
import subprocess
import sys


def run_command(command: list, cwd: str) -> None:
    """Run a shell command and stream its output to the console.

    Args:
        command: List of command arguments to execute.
        cwd: Working directory to run the command in.

    Raises:
        subprocess.CalledProcessError: If the command returns a non‑zero exit
        status.
    """
    print(f"\nRunning: {' '.join(command)} (in {cwd})")
    process = subprocess.Popen(command, cwd=cwd)
    process.communicate()
    if process.returncode != 0:
        raise subprocess.CalledProcessError(process.returncode, command)


def build_variant(project_root: str, flavor: str) -> None:
    """Assemble a specific release variant using Gradle.

    Args:
        project_root: Path to the repository root where `gradlew` resides.
        flavor: The build flavor to assemble ("sentry" or "nonalytics").
    """
    variant_task = f"assemble{flavor.capitalize()}Release"
    run_command(["./gradlew", variant_task], cwd=project_root)
    apk_path = os.path.join(
        project_root,
        "app",
        "build",
        "outputs",
        "apk",
        flavor,
        "release",
        f"app-{flavor}-release.apk",
    )
    if os.path.exists(apk_path):
        print(f"Built APK located at: {apk_path}")
    else:
        print(f"Warning: expected APK not found at {apk_path}")


def parse_args(argv: list) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build AAIdrive release variants")
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Perform a clean build before assembling the APKs",
    )
    parser.add_argument(
        "--flavor",
        choices=["sentry", "nonalytics", "all"],
        default="all",
        help="Select which flavor to build",
    )
    return parser.parse_args(argv)


def main(argv: list) -> None:
    args = parse_args(argv)
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))

    if args.clean:
        run_command(["./gradlew", "clean"], cwd=project_root)

    flavors = ["sentry", "nonalytics"] if args.flavor == "all" else [args.flavor]
    for flavor in flavors:
        print(f"\nAssembling {flavor} release variant...")
        build_variant(project_root, flavor)

    print("\nBuild process completed.")


if __name__ == "__main__":
    try:
        main(sys.argv[1:])
    except subprocess.CalledProcessError as e:
        sys.exit(e.returncode)
