package gov.cms.ab2d.contracts.model;

import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {
    @NotNull
    private String contractNumber;

    @NotNull
    private String contractName;

    @EqualsAndHashCode.Exclude  // contractNumber is sufficient, breaks on Windows due sub-seconds not matching
    private OffsetDateTime attestedOn;

    private Contract.ContractType contractType;

    public boolean hasDateIssue() {
        return Contract.ContractType.CLASSIC_TEST == contractType;
    }
}
