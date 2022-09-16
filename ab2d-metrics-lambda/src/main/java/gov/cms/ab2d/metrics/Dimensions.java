package gov.cms.ab2d.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Dimensions {
    private String value;
    private String name;
}
