#!/usr/bin/env python3
"""Send Minecraft Java RCON commands to the isolated visual-validation server."""
import os
import socket
import struct
import sys
import time

HOST = os.environ.get("MC_RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("MC_RCON_PORT", "25575"))
PASSWORD = os.environ["RCON_PASSWORD"]


def read_exact(sock, size):
    chunks = bytearray()
    while len(chunks) < size:
        chunk = sock.recv(size - len(chunks))
        if not chunk:
            raise ConnectionError("RCON connection closed early")
        chunks.extend(chunk)
    return bytes(chunks)


def read_packet(sock):
    size = struct.unpack("<i", read_exact(sock, 4))[0]
    if size < 10 or size > 4 * 1024 * 1024:
        raise ValueError(f"invalid RCON packet length: {size}")
    body = read_exact(sock, size)
    request_id, packet_type = struct.unpack("<ii", body[:8])
    return request_id, packet_type, body[8:-2].decode("utf-8", "replace")


def send_packet(sock, request_id, packet_type, payload):
    text = payload.encode("utf-8") + b"\0\0"
    body = struct.pack("<ii", request_id, packet_type) + text
    sock.sendall(struct.pack("<i", len(body)) + body)


def connect():
    deadline = time.monotonic() + 30
    last_error = None
    while time.monotonic() < deadline:
        try:
            sock = socket.create_connection((HOST, PORT), timeout=5)
            sock.settimeout(10)
            send_packet(sock, 1, 3, PASSWORD)
            request_id, _, _ = read_packet(sock)
            if request_id != 1:
                sock.close()
                raise PermissionError("Minecraft RCON authentication failed")
            return sock
        except (OSError, ConnectionError) as error:
            last_error = error
            time.sleep(1)
    raise TimeoutError(f"RCON did not become ready: {last_error}")


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: rcon.py <command> [command ...]")
    with connect() as sock:
        for index, command in enumerate(sys.argv[1:], start=2):
            send_packet(sock, index, 2, command)
            request_id, _, response = read_packet(sock)
            if request_id != index:
                raise RuntimeError(f"unexpected RCON response id {request_id}")
            print(response)
            if any(error in response for error in (
                "Incorrect argument for command", "That position is not loaded",
                "Unknown or incomplete command",
            )):
                raise RuntimeError(f"Minecraft rejected RCON command {command!r}: {response}")


if __name__ == "__main__":
    main()
