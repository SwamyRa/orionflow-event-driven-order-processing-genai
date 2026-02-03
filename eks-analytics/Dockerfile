FROM public.ecr.aws/amazoncorretto/amazoncorretto:17-al2023

WORKDIR /app

COPY target/eks-analytics-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
