"""
update_translations.py
=======================

This script automates the process of fetching the latest translated
strings for the AAIDrive project from Crowdin and merging them into
the Android resource directories. Crowdin is a popular localization
platform that hosts the community‑driven translations used by
AAIDrive. By automating the build and download of translation files,
developers can ensure that the app ships with the most up‑to‑date
translations without manually interacting with the web UI.

Usage
-----

Run the script from the root of your AAIDrive checkout, or
specify the target resource directory with ``--target``.

```
python3 update_translations.py --project-id <project-id> \
    --branch main --target app/src/main/res
```

The script requires a Crowdin API token with permissions to build
and download translation bundles. Set the token in the environment
variable ``CROWDIN_API_TOKEN``. If you wish to use an API base
URL other than the default ``https://api.crowdin.com/api/v2``, set
``CROWDIN_API_BASE``.

Details
-------

1. **Start translation build**: Initiates a build for the specified
   branch on Crowdin. Crowdin prepares a ZIP archive containing all
   translation resources.
2. **Poll build status**: The script polls Crowdin until the build is
   finished, providing progress feedback in the console.
3. **Download translations**: Once ready, the build artifact is
   downloaded and saved to a temporary location.
4. **Extract translations**: The ZIP archive is extracted and the
   relevant ``strings.xml`` files are copied into the target
   Android resource directories (e.g., ``values-de/strings.xml``).

This script is intentionally general so that additional localization
targets (such as different product flavors or modules) can be added
easily. All HTTP interactions include helpful error messages to
facilitate troubleshooting.
"""

import argparse
import json
import os
import sys
import time
import zipfile
from io import BytesIO
from pathlib import Path
from typing import Optional

import requests


API_BASE = os.getenv("CROWDIN_API_BASE", "https://api.crowdin.com/api/v2")


def start_translation_build(project_id: str, branch: str, token: str) -> str:
    """Initiate a translations build on Crowdin and return the build ID.

    Args:
        project_id: The numeric ID of the Crowdin project.
        branch: The branch name to build translations for.
        token: The Crowdin API token.

    Returns:
        The build ID as a string.
    """
    url = f"{API_BASE}/projects/{project_id}/translations/builds"
    headers = {"Authorization": f"Bearer {token}"}
    payload = {"branchId": None, "branch": branch}
    resp = requests.post(url, headers=headers, json=payload)
    if resp.status_code not in (200, 201):
        raise RuntimeError(
            f"Failed to start translation build: {resp.status_code} {resp.text}"
        )
    build = resp.json()
    return build.get("data", {}).get("id")


def poll_build_status(project_id: str, build_id: str, token: str) -> None:
    """Poll Crowdin until the build finishes.

    Args:
        project_id: Crowdin project ID.
        build_id: The ID of the build to poll.
        token: Crowdin API token.
    """
    url = f"{API_BASE}/projects/{project_id}/translations/builds/{build_id}"
    headers = {"Authorization": f"Bearer {token}"}
    while True:
        resp = requests.get(url, headers=headers)
        if resp.status_code != 200:
            raise RuntimeError(
                f"Failed to fetch build status: {resp.status_code} {resp.text}"
            )
        data = resp.json().get("data", {})
        status = data.get("status")
        progress = data.get("progress")
        print(f"Build status: {status} (progress: {progress}%)", end="\r")
        if status == "finished":
            print()  # newline after finished
            return
        time.sleep(5)


def download_translations(project_id: str, build_id: str, token: str) -> bytes:
    """Download the built translations ZIP archive from Crowdin.

    Args:
        project_id: Crowdin project ID.
        build_id: The ID of the finished build.
        token: Crowdin API token.

    Returns:
        The binary contents of the ZIP file.
    """
    # Request a download URL from Crowdin
    url = f"{API_BASE}/projects/{project_id}/translations/builds/{build_id}/download"
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.get(url, headers=headers)
    if resp.status_code != 200:
        raise RuntimeError(
            f"Failed to obtain download URL: {resp.status_code} {resp.text}"
        )
    download_url = resp.json().get("data", {}).get("url")
    if not download_url:
        raise RuntimeError("Download URL missing from response")
    print("Downloading translation archive...")
    binary = requests.get(download_url).content
    return binary


def extract_and_copy(zip_bytes: bytes, target_dir: Path) -> None:
    """Extract translation files from the given ZIP and copy into target directory.

    The ZIP is expected to contain Android resource directories such as
    ``values-es/strings.xml``. This function will replace existing
    translations in the target directory and create directories as
    necessary.

    Args:
        zip_bytes: The binary contents of the ZIP file.
        target_dir: The root directory where Android resources live (e.g., ``app/src/main/res``).
    """
    with zipfile.ZipFile(BytesIO(zip_bytes)) as zf:
        for member in zf.namelist():
            # Only handle string resources
            if member.endswith("strings.xml"):
                locale_dir, filename = os.path.split(member)
                target_path = target_dir / locale_dir / filename
                target_path.parent.mkdir(parents=True, exist_ok=True)
                with zf.open(member) as src, open(target_path, "wb") as dest:
                    dest.write(src.read())
                print(f"Updated {target_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Update project translations from Crowdin")
    parser.add_argument(
        "--project-id",
        required=True,
        help="The Crowdin project ID to build translations for",
    )
    parser.add_argument(
        "--branch",
        default="main",
        help="Branch name in Crowdin to build translations for (default: main)",
    )
    parser.add_argument(
        "--target",
        default="app/src/main/res",
        help="Target directory for Android resources (default: app/src/main/res)",
    )
    parser.add_argument(
        "--no-extract",
        action="store_true",
        help="Only trigger the build and download the archive without extracting",
    )
    args = parser.parse_args()

    token = os.getenv("CROWDIN_API_TOKEN")
    if not token:
        print("Error: CROWDIN_API_TOKEN environment variable not set.", file=sys.stderr)
        sys.exit(1)

    print(f"Starting translation build for project {args.project_id} on branch {args.branch}...")
    try:
        build_id = start_translation_build(args.project_id, args.branch, token)
        print(f"Build started. ID: {build_id}")
        poll_build_status(args.project_id, build_id, token)
        zip_data = download_translations(args.project_id, build_id, token)
        archive_path = Path("crowdin_translations.zip")
        with open(archive_path, "wb") as f:
            f.write(zip_data)
        print(f"Downloaded translations archive to {archive_path}")
        if not args.no_extract:
            target_dir = Path(args.target)
            extract_and_copy(zip_data, target_dir)
            print("Translations updated.")
    except Exception as e:
        print(f"Error while updating translations: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
