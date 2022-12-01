package gov.cms.ab2d.contracts.Feign;

import gov.cms.ab2d.contracts.model.ContractAPI;

@FeignClient(value = "contract")
public interface ContractFeign extends ContractAPI {

}
