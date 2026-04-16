# Rebuild from the last known-good local runtime image so Docker doesn't need
# to reach Docker Hub when the registry is unavailable.
FROM in2033_ipos-ca-app:latest
WORKDIR /app
COPY target/IPOS-CA-1.0-SNAPSHOT.jar app.jar
COPY sql/ ./sql/
COPY database/ ./database/
COPY start.sh .
RUN chmod +x start.sh

EXPOSE 5900 6080 8082

ENTRYPOINT ["./start.sh"]
