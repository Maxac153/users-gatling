package models.profile;

import lombok.Data;

import java.util.List;

@Data
public class Canvas {
    private List<Elements> element;
    private CommonSettings commonSettings;
}
