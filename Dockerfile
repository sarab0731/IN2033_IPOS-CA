# Reuse the local CA runtime image so rebuilds do not depend on Docker Hub.
FROM in2033_ipos-ca-app:latest
WORKDIR /app

COPY target/IPOS-CA-1.0-SNAPSHOT.jar app.jar
COPY sql/ ./sql/
COPY database/ ./database/
COPY start.sh .
RUN chmod +x start.sh

EXPOSE 5900 6080 8082

ENTRYPOINT ["./start.sh"]
