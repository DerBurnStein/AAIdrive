#!/usr/bin/env python3
"""
Generate Release Notes
======================

This script helps generate release notes for AAIdrive by querying the
GitHub API for the commit history between two tags or commits. The
output is written to stdout in Markdown format, suitable for use in
GitHub Releases or changelog documents.

This script requires a GitHub API token to avoid rate limits. Set the
environment variable `GITHUB_TOKEN` to a personal access token with
`repo` scope. Without a token, the script will attempt unauthenticated
requests, which may hit rate limits on larger repositories.

Usage:
    python generate_release_notes.py --repo DerBurnStein/AAIdrive \
       --since v1.4.2 --until v1.4.3

If `--since` is omitted, the script uses the previous tag chronologically
before `--until`. If `--until` is omitted, the script uses the current
HEAD of the default branch.
"""

import argparse
import datetime
import os
import sys
from typing import List, Optional

try:
    import requests  # type: ignore
except ImportError:
    requests = None  # type: ignore


GITHUB_API = "https://api.github.com"



def get_commits(repo: str, since: Optional[str], until: Optional[str], token: Optional[str]) -> List[dict]:
    """Fetch the list of commits between two references.

    Args:
        repo: Repository in the form "owner/repo".
        since: Tag or SHA to start from (exclusive).
        until: Tag or SHA to end at (inclusive).
        token: Optional GitHub API token.

    Returns:
        A list of commit dictionaries.
    """
    if requests is None:
        raise RuntimeError("The requests library is required to use this script.")
    headers = {"Accept": "application/vnd.github.v3+json"}
    if token:
        headers["Authorization"] = f"token {token}"
    url = f"{GITHUB_API}/repos/{repo}/compare/{since}...{until}" if since else f"{GITHUB_API}/repos/{repo}/commits"
    params = {}
    if since is None and until:
        params["sha"] = until
    commits: List[dict] = []
    resp = requests.get(url, headers=headers, params=params, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    if since:
        commits = data.get("commits", [])
    else:
        commits = data
    return commits


def format_release_notes(commits: List[dict]) -> str:
    """Format a list of commits into release notes in Markdown."""
    lines = ["## Changes\n"]
    for commit in commits:
        message = commit.get("commit", {}).get("message", "").split("\n")[0]
        sha = commit.get("sha", "")[:7]
        author = commit.get("commit", {}).get("author", {}).get("name", "")
        date_str = commit.get("commit", {}).get("author", {}).get("date", "")
        try:
            date = datetime.datetime.fromisoformat(date_str.rstrip("Z"))
            date_fmt = date.strftime("%Y-%m-%d")
        except ValueError:
            date_fmt = date_str
        lines.append(f"* {message} (`{sha}`) by {author} on {date_fmt}")
    return "\n".join(lines)


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate release notes from GitHub commits")
    parser.add_argument("--repo", required=True, help="Repository in the form owner/repo")
    parser.add_argument("--since", help="Start tag or commit (exclusive)")
    parser.add_argument("--until", help="End tag or commit (inclusive)")
    return parser.parse_args(argv)


def main(argv: List[str]) -> None:
    args = parse_args(argv)
    token = os.environ.get("GITHUB_TOKEN")
    if requests is None:
        print("Error: The requests library is not installed. Please install requests to use this script.")
        sys.exit(1)
    try:
        commits = get_commits(args.repo, args.since, args.until, token)
    except Exception as e:
        print(f"Failed to fetch commits: {e}")
        sys.exit(1)
    notes = format_release_notes(commits)
    print(notes)


if __name__ == "__main__":
    main(sys.argv[1:])
