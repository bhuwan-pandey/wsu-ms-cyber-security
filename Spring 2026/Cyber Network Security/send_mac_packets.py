#!/usr/bin/env python3
from scapy.all import Ether, sendp, get_if_hwaddr
import sys

def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <destination-mac> [iface] [count]")
        sys.exit(1)

    dst_mac = sys.argv[1]
    iface = sys.argv[2] if len(sys.argv) > 2 else "eth0"
    count = int(sys.argv[3]) if len(sys.argv) > 3 else 5

    # Get the source MAC of the interface
    try:
        src_mac = get_if_hwaddr(iface)
    except Exception as e:
        print(f"[!] Failed to get MAC for interface '{iface}': {e}")
        sys.exit(1)

    # Generate payloads: "AAAA", "BBBB", "CCCC", etc.
    payloads = [chr(65 + i) * 4 for i in range(count)]  # 65 = 'A'

    print(f"[+] Using iface={iface}, src={src_mac}, dst={dst_mac}, count={count}")
    for idx, payload in enumerate(payloads, start=1):
        pkt = Ether(src=src_mac, dst=dst_mac) / payload
        print(f"[+] Sending packet {idx}: src={src_mac}, dst={dst_mac}, payload={payload}")
        sendp(pkt, iface=iface, verbose=False)

    print("[+] Done.")

if __name__ == "__main__":
    main()