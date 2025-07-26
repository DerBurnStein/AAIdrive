Automation Tools for AAIDrive
=============================

This directory contains a collection of Python scripts designed to
streamline development and release management for the AAIDrive
project. Each script addresses a specific repetitive task so that
maintainers can focus on building features rather than babysitting
tooling.

Scripts
-------

### build_release.py

Automates the Gradle build process for AAIDrive. It supports
assembling multiple release variants (for example, with or without
analytics) and can optionally perform a clean build. See the script’s
`--help` output for usage details.

### generate_release_notes.py

Generates Markdown release notes by querying the GitHub API for
commits between two points in history. This helps with composing
changelogs for new versions. The script requires a personal access
token via the `GITHUB_TOKEN` environment variable.

### update_translations.py

Integrates with Crowdin to fetch the latest translations and merge
them into the Android resource directories. It triggers a build on
Crowdin, waits for the build to finish, downloads the result, and
extracts the `strings.xml` files into `app/src/main/res`. It
requires a Crowdin API token via the `CROWDIN_API_TOKEN` environment
variable.

### simulate_car_data.py

Emulates live vehicle sensor data such as speed, RPM, fuel level, and
coolant temperature. Developers can stream the generated JSON
messages to the console, to a log file, or over a WebSocket
connection for integration tests. The script is highly configurable
through command‑line arguments.

Contributing
------------

If you have an idea for another automation task, feel free to add a
new script here. Please include a brief docstring explaining the
purpose of the script and update this README accordingly.
