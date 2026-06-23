#!/usr/bin/env python3
"""Simple CLI to chat with an Amazon Bedrock Agent from VS Code terminal.

Usage examples:
  python run_agent.py --agent-id CLSYHNOYIW --alias-name prod --region us-east-1 --profile default
  python run_agent.py --agent-id CLSYHNOYIW --alias-id PEAAX0JY4B --prompt "List all records"
"""

from __future__ import annotations

import argparse
import sys
import uuid
from typing import Optional

import boto3
from botocore.exceptions import BotoCoreError, ClientError


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Invoke a Bedrock Agent from the terminal.")
    parser.add_argument("--region", default="us-east-1", help="AWS region (default: us-east-1)")
    parser.add_argument("--profile", default=None, help="AWS profile name from ~/.aws/credentials")
    parser.add_argument("--agent-id", required=True, help="Bedrock Agent ID")
    parser.add_argument("--alias-id", default=None, help="Agent Alias ID (preferred)")
    parser.add_argument("--alias-name", default="prod", help="Agent alias name if alias-id is not provided")
    parser.add_argument("--session-id", default=None, help="Session ID (default: generated UUID)")
    parser.add_argument("--prompt", default=None, help="Single prompt text. If omitted, interactive chat starts.")
    parser.add_argument("--trace", action="store_true", help="Print trace event markers when available")
    return parser.parse_args()


def build_session(region: str, profile: Optional[str]):
    if profile:
        return boto3.session.Session(region_name=region, profile_name=profile)
    return boto3.session.Session(region_name=region)


def resolve_alias_id(session: boto3.session.Session, agent_id: str, alias_name: str) -> str:
    control = session.client("bedrock-agent")
    paginator = control.get_paginator("list_agent_aliases")

    for page in paginator.paginate(agentId=agent_id):
        for item in page.get("agentAliasSummaries", []):
            if item.get("agentAliasName") == alias_name:
                return item["agentAliasId"]

    raise ValueError(f"Could not find alias '{alias_name}' for agent '{agent_id}'.")


def invoke_prompt(runtime_client, agent_id: str, alias_id: str, session_id: str, prompt: str, show_trace: bool) -> str:
    response = runtime_client.invoke_agent(
        agentId=agent_id,
        agentAliasId=alias_id,
        sessionId=session_id,
        inputText=prompt,
    )

    chunks = []
    for event in response.get("completion", []):
        if "chunk" in event and "bytes" in event["chunk"]:
            raw = event["chunk"]["bytes"]
            if isinstance(raw, (bytes, bytearray)):
                chunks.append(raw.decode("utf-8", errors="ignore"))
            else:
                chunks.append(str(raw))
        elif show_trace and "trace" in event:
            print("[trace] received trace event", file=sys.stderr)

    return "".join(chunks).strip()


def main() -> int:
    args = parse_args()

    try:
        session = build_session(args.region, args.profile)
        runtime = session.client("bedrock-agent-runtime")

        alias_id = args.alias_id
        if not alias_id:
            alias_id = resolve_alias_id(session, args.agent_id, args.alias_name)

        session_id = args.session_id or str(uuid.uuid4())
        print(f"Using session-id: {session_id}")
        print(f"Using alias-id: {alias_id}")

        if args.prompt:
            output = invoke_prompt(runtime, args.agent_id, alias_id, session_id, args.prompt, args.trace)
            print(output)
            return 0

        print("Interactive mode. Type 'exit' to quit.")
        while True:
            try:
                prompt = input("you> ").strip()
            except (EOFError, KeyboardInterrupt):
                print()
                break

            if not prompt:
                continue
            if prompt.lower() in {"exit", "quit"}:
                break

            output = invoke_prompt(runtime, args.agent_id, alias_id, session_id, prompt, args.trace)
            print(f"agent> {output}")

        return 0

    except ValueError as exc:
        print(f"Configuration error: {exc}", file=sys.stderr)
        return 2
    except (ClientError, BotoCoreError) as exc:
        print(f"AWS error: {exc}", file=sys.stderr)
        return 3
    except Exception as exc:  # noqa: BLE001
        print(f"Unexpected error: {exc}", file=sys.stderr)
        return 4


if __name__ == "__main__":
    raise SystemExit(main())

