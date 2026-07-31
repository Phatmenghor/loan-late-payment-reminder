FROM account_online:1.0
COPY internal-service.jar /app/internal-service.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=25.0", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-Duser.timezone=Asia/Phnom_Penh", "-jar", "/app/internal-service.jar"]
