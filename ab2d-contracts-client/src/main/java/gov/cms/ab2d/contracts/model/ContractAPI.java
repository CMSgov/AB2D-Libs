package gov.cms.ab2d.contracts.model;

import gov.cms.ab2d.contracts.model.Contract;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface ContractAPI {
    @GetMapping("/contract")
    List<ContractDTO> getContracts(@RequestParam("contractId") Optional<Long> contractId);

    @PutMapping("/contract")
    void updateContract(@RequestBody Contract contract);

    @GetMapping("/contract/{contractNumber}")
    List<ContractDTO> getContractByNumber(@PathVariable("contractNumber") Optional<String> contractNumber);
}

