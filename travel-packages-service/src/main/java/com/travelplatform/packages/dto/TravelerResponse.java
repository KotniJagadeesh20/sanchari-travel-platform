package com.travelplatform.packages.dto;

import com.travelplatform.packages.entity.PackageTraveler;

public class TravelerResponse {
    private String name;
    private Integer age;

    public static TravelerResponse from(PackageTraveler t) {
        TravelerResponse r = new TravelerResponse();
        r.name = t.getName();
        r.age = t.getAge();
        return r;
    }

    public String getName() { return name; }
    public Integer getAge() { return age; }
}
