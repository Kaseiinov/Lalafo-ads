package com.example.demo.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ad {

    private String imageUrl;
    private String adUrl;
    private String title;
    private String price;
    private String city;
    private String date;

}
