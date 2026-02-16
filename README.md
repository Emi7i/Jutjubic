hallo!!!

# Jutjubić Projekat - ISA

_rade:_ 
<br> 1. _Miloš Herceg RA2/2022_ 
<br> 2. _Emilija Opsenica RA108/2022_

## Useful links:
- Health check for containers: http://localhost:8080/actuator/health
- Load balancer showcase: http://localhost:8080/api/test/database
- Grafana: http://localhost:3000/
- Prometheus: http://localhost:9090/

To run frontend manually, run: <br>
    `cd frontend; ng serve --host 0.0.0.0 --port 4200`

To run video-event-consumer manually, run:<br>
    `cd video-event-consumer; mvn clean package`<br>
    `java -jar .\target\video-event-consumer-1.0.0-jar-with-dependencies.jar`
