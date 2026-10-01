"""PC audio server: captures system audio (loopback) and streams raw PCM over TCP.
Setup:  pip install soundcard numpy     |  Run: python server.py [port]
Windows: works out of the box. Linux: PulseAudio/PipeWire monitor. macOS: install BlackHole and set it as output.
"""
import socket, sys, threading
import numpy as np
import soundcard as sc

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 5555
RATE, CH, FRAMES = 44100, 2, 1024
clients, lock = [], threading.Lock()

def local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try: s.connect(("8.8.8.8", 80)); return s.getsockname()[0]
    except Exception: return "127.0.0.1"
    finally: s.close()

def acceptor(srv):
    while True:
        c, addr = srv.accept()
        c.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        with lock: clients.append(c)
        print("Client connected:", addr[0])

srv = socket.socket(); srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
srv.bind(("0.0.0.0", PORT)); srv.listen(5)
threading.Thread(target=acceptor, args=(srv,), daemon=True).start()
print(f"Enter this on your phone ->  IP: {local_ip()}   Port: {PORT}")

spk = sc.default_speaker()
mic = sc.get_microphone(id=str(spk.name), include_loopback=True)
with mic.recorder(samplerate=RATE, channels=CH, blocksize=FRAMES) as rec:
    while True:
        data = rec.record(numframes=FRAMES)
        pcm = (np.clip(data, -1, 1) * 32767).astype("<i2").tobytes()
        with lock:
            for c in clients[:]:
                try: c.sendall(pcm)
                except Exception: clients.remove(c); c.close()
