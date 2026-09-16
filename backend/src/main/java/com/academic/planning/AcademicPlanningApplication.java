package com.academic.planning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.academic.planning.mapper")
@SpringBootApplication
public class AcademicPlanningApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicPlanningApplication.class, args);
    }
}
