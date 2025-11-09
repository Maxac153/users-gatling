package ru.gatling.models.profile;

import lombok.Data;

import java.util.HashMap;

@Data
public class Canvas {
    private HashMap<String, TestParam> element;
    private CommonSettings commonSettings;
}
