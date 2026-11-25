#!/usr/bin/env python3
"""Check that the TV's network ADB port is closed, after turning debugging off (no nc needed).

    adb-closed.py <tv ip[:port]> [port]   port defaults to TV_PORT or 5555; the IP to TV_HOST

Exits 0 when the TV refuses the connection, 1 when the port is still open, and 2 when the check
could not tell: nothing answered (the TV off, asleep or rebooting, at another address, or out of reach
from here), nothing listened on 127.0.0.1, or the check itself could not run (a name that does not
resolve or is not a hostname, no route to the TV). Only the TV's own refusal shows the port closed.

A TV_PORT exported with TV_HOST=127.0.0.1, or set to relay.py's 15555, is a relay's (tools/relay.md), so
it is not used for a TV's own IP. 127.0.0.1 is this computer: a relay answers there for itself, not for
the TV, so a refusal there is not the TV's.
"""
import ipaddress
import os
import socket
import struct
import sys

TIMEOUT_S = 3.0
RELAY_PORT = 15555   # relay.py's default --port, the TV_PORT it tells the tools to use


HANDSHAKE_S = 3.0
A_CNXN, A_AUTH, A_STLS = 0x4E584E43, 0x48545541, 0x534C5453
ADB_ANSWERS = {A_CNXN: "CNXN", A_AUTH: "AUTH", A_STLS: "STLS"}


def greeting():
    """The CNXN a host sends first: protocol version 1.0.0.1, 256 KiB max payload, banner host::."""
    payload = b"host::\0"
    return struct.pack("<6I", A_CNXN, 0x01000001, 256 * 1024, len(payload), sum(payload) & 0xFFFFFFFF,
                       A_CNXN ^ 0xFFFFFFFF) + payload


def adb_answer(header):
    """The ADB command a 24-byte header carries (CNXN, AUTH or STLS), or None when it is not ADB's."""
    if len(header) != 24:
        return None
    command, _, _, _, _, magic = struct.unpack("<6I", header)
    if command in ADB_ANSWERS and magic == command ^ 0xFFFFFFFF:
        return ADB_ANSWERS[command]
    return None


def short_reply(header):
    """A partial protocol header never establishes whether the service is ADB."""
    for command, name in ADB_ANSWERS.items():
        if 0 < len(header) < 24 and header[:min(4, len(header))] == struct.pack("<I", command)[:min(4, len(header))]:
            return "unknown", f"it began an ADB answer ({name}) and stopped after {len(header)} of 24 bytes"
    return "unknown", f"it returned an incomplete or unrecognized header ({len(header)} bytes)"


def ask_adb(sock):
    """Greet an open port: ("adb", command), ("not adb", why) or ("unknown", why).

    Silence is "unknown", not "not adb": a port that waits for more than this greeting could still be
    anything, and a stand-in or a half-open socket that cannot send says nothing about the TV either.
    """
    header = b""
    try:
        sock.getpeername()   # a socket with no peer (a test's stand-in) has nothing to greet
    except OSError as e:
        return "unknown", f"the greeting could not be sent ({e})"
    try:
        sock.settimeout(HANDSHAKE_S)
        sock.sendall(greeting())
        while len(header) < 24:
            chunk = sock.recv(24 - len(header))
            if not chunk:
                break
            header += chunk
    except socket.timeout:
        return short_reply(header) if header else ("unknown", f"it said nothing within {HANDSHAKE_S:.0f} s")
    except OSError as e:
        return "unknown", f"the greeting could not be sent ({e})"
    command = adb_answer(header)
    if command:
        return "adb", command
    if not header:
        return "unknown", "it hung up before returning a protocol header"
    if len(header) == 24:
        return "not adb", "it returned a complete header that is not ADB"
    return short_reply(header)


def this_computer(host):
    """127.0.0.1, localhost or another loopback address: where a relay listens, never the TV.

    Every way the socket API takes to write it counts: localhost with the root's trailing dot, the short
    IPv4 forms (127.1, 2130706433, 0x7f000001), ::1 and an IPv4-mapped ::ffff:127.0.0.1, and 0.0.0.0 or ::,
    which connect to this computer as well.
    """
    name = host.lower()
    if name.endswith("."):
        name = name[:-1]
    if name == "localhost" or name.endswith(".localhost"):
        return True
    try:
        address = ipaddress.ip_address(name.split("%", 1)[0])
    except ValueError:
        try:
            address = ipaddress.IPv4Address(socket.inet_aton(name))
        except (OSError, ValueError):
            return False
    # Before Python 3.13 an IPv4-mapped loopback is not is_loopback.
    if getattr(address, "ipv4_mapped", None):
        address = address.ipv4_mapped
    return address.is_loopback or address.is_unspecified


def split_host_port(endpoint):
    """Split host:port or [ipv6]:port into (host, port_or_none)."""
    if endpoint.startswith("["):
        end = endpoint.find("]")
        if end != -1:
            host = endpoint[1:end]
            rest = endpoint[end + 1:]
            if rest.startswith(":"):
                return host, rest[1:]
            return host, None
    elif endpoint.count(":") == 1:
        host, port = endpoint.split(":", 1)
        return host, port
    return endpoint, None


def main():
    args = sys.argv[1:]
    if len(args) > 2 or args[:1] in (["-h"], ["--help"]):
        sys.stderr.write(__doc__)
        return 2
    raw_host = args[0] if args else os.environ.get("TV_HOST", "")
    if not raw_host:
        sys.stderr.write("usage: adb-closed.py <tv ip> [port]\n")
        return 2
    host, endpoint_port = split_host_port(raw_host)
    port_from_env = False
    if len(args) == 2:
        port_str = args[1]
    elif len(args) == 1 and endpoint_port is not None:
        port_str = endpoint_port
    else:
        port_from_env = not endpoint_port and bool(os.environ.get("TV_PORT"))
        port_str = endpoint_port or os.environ.get("TV_PORT") or "5555"

    try:
        port = int(port_str)
        if not (1 <= port <= 65535):
            raise ValueError
    except ValueError:
        sys.stderr.write(f"not a port number: {port_str!r}\n")
        return 2

    # relay.py's rule: a TV_PORT exported beside TV_HOST=127.0.0.1, or the relay's own 15555, is a relay's,
    # not the TV's. With the TV's IP typed at the closing step this would check IP:15555, which the TV
    # refuses, and say "closed" while 5555 is open. A TV_PORT exported beside the TV's own IP (5877 on the
    # Wanbo T6R Max) still counts.
    env_host = split_host_port(os.environ.get("TV_HOST", ""))[0]
    local = this_computer(host)
    if port_from_env and not local and (port == RELAY_PORT or this_computer(env_host)):
        sys.stderr.write(f"TV_PORT={port} is a relay's port, set for the tools: unset TV_HOST TV_PORT in this "
                         f"terminal, or give the TV's port, e.g.  tools/adb-closed.py {host} 5555\n")
        return 2
    # Through a relay the answer is the relay's: "still open" while it runs, refused once it stops,
    # whatever the TV's own port is doing. So a refusal on this computer is never the TV's.
    relay = (f"{host} is this computer: a relay answers for itself, not for the TV. Check the TV's own IP\n"
             "from an ordinary terminal:  tools/adb-closed.py <tv ip>\n" if local else "")

    # Winsock getaddrinfo can reject short numeric IPv4 forms even though inet_aton accepts them.
    # Normalize only the transport address; diagnostics retain the exact address the person supplied.
    connection_host = host
    try:
        connection_host = str(ipaddress.IPv4Address(socket.inet_aton(host)))
    except (OSError, ValueError):
        pass
    try:
        sock = socket.create_connection((connection_host, port), timeout=TIMEOUT_S)
    except ConnectionRefusedError:
        if local:
            sys.stderr.write(f"could not check {host}:{port}: nothing listens there, which says nothing about "
                             "the TV.\n" + relay)
            return 2
        print(f"{host}:{port} is closed: the TV refused the connection.")
        return 0
    except socket.timeout:
        # No answer is not a refusal, but it was exit 0 here. Port 5555 times out with ADB still on when
        # the TV is off, asleep or rebooting, at a new address, on another VLAN, behind a VPN's firewall or
        # out of an assistant's sandbox (docs/TROUBLESHOOTING.md, tools/relay.md), so the closing step would
        # be told "closed" for a TV this never reached.
        sys.stderr.write(f"could not check {host}:{port}: it did not answer in {TIMEOUT_S:.0f} s, and that "
                         "proves nothing. The TV\nmay be off, asleep or still rebooting, at another address, "
                         "or out of reach from here (a VPN,\nanother VLAN, an assistant's sandbox: run this "
                         "from an ordinary terminal). Only a refused\nconnection shows the port closed.\n"
                         + relay)
        return 2
    except UnicodeError:
        # getaddrinfo runs the name through the idna codec, which raises UnicodeError on an empty or
        # over-long label (192.168.1..50, a 70-character label) instead of the gaierror an unresolvable
        # name gives. Left uncaught it printed a traceback and exited 1 - the code this tool reserves for
        # "the port is still open" - so a typo in the address read as an open port at the closing step.
        sys.stderr.write(f"could not check {host}:{port}: not a hostname. Check the address.\n")
        return 2
    except OSError as e:
        sys.stderr.write(f"could not check {host}:{port} ({e}). Is the TV on and on this network?\n")
        return 2
    try:
        verdict, detail = ("unknown", "local relay is not the TV") if local else ask_adb(sock)
    finally:
        sock.close()
    if verdict == "adb":
        sys.stderr.write(f"{host}:{port} is still open: network ADB answered {detail}.\n" + relay)
    elif verdict == "not adb":
        sys.stderr.write(f"{host}:{port} is still open, but did not answer as ADB: {detail}.\n"
                         "This does not establish whether ADB is open on another port.\n" + relay)
    else:
        sys.stderr.write(f"{host}:{port} is still open; its protocol is unknown ({detail}).\n"
                         "An open TCP port alone does not prove network ADB is on.\n" + relay)
    return 1


if __name__ == "__main__":
    sys.exit(main())
