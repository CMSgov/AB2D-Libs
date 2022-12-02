package gov.cms.ab2d.contracts.Feign;

import gov.cms.ab2d.contracts.model.ContractAPI;
import org.springframework.cloud.netflix.feign.FeignClient;

@FeignClient(value = "contract")
public interface ContractFeignClient extends ContractAPI { }
