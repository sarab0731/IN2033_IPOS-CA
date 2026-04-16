#!/bin/bash
set -e

# Write db.properties from environment variables (overrides any bundled copy)
cat > /app/db.properties <<EOF
db.url=${DB_URL:-jdbc:mysql://mysql:3306/IPOS-CA}
db.user=${DB_USER:-root}
db.password=${DB_PASSWORD:-Password123}
EOF

export DISPLAY=:1

# Clear stale X11 lock/socket files from previous runs.
rm -f /tmp/.X1-lock
rm -f /tmp/.X11-unix/X1
mkdir -p /tmp/.X11-unix
chmod 1777 /tmp/.X11-unix

# Start virtual framebuffer display
Xvfb "$DISPLAY" -screen 0 1280x800x24 -ac +extension GLX +render -noreset &

for _ in $(seq 1 10); do
  if xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

# Start VNC server (no password, shared so multiple viewers can connect)
x11vnc -display "$DISPLAY" -nopw -listen 0.0.0.0 -rfbport 5900 -forever -shared -quiet 2>/dev/null &
sleep 1

# Start noVNC — serves the web client and proxies to VNC on 5900
websockify --web=/usr/share/novnc/ 6080 localhost:5900 &

echo "============================================"
echo "  IPOS-CA GUI → http://localhost:6080/vnc.html"
echo "  VNC direct   → localhost:5900 (no password)"
echo "  API server   → http://localhost:8082"
echo "============================================"

# Launch the application (classpath includes all Maven dependencies)
exec java \
  -Djava.awt.headless=false \
  -Dawt.useSystemAAFontSettings=on \
  -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel \
  -cp "app.jar:dependency/*" \
  app.Main
