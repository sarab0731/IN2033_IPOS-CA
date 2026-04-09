#!/bin/bash
set -e

# Write db.properties from environment variables (overrides any bundled copy)
cat > /app/db.properties <<EOF
db.url=${DB_URL:-jdbc:mysql://mysql:3306/IPOS-CA}
db.user=${DB_USER:-root}
db.password=${DB_PASSWORD:-Password123}
EOF

# Start virtual framebuffer display
Xvfb :1 -screen 0 1280x800x24 -ac +extension GLX +render -noreset &
export DISPLAY=:1
sleep 1

# Start VNC server (no password, shared so multiple viewers can connect)
x11vnc -display :1 -nopw -listen 0.0.0.0 -rfbport 5900 -forever -shared -quiet &
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