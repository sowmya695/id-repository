package io.mosip.credentialstore.service;

import org.springframework.stereotype.Service;

import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;


// TODO: Auto-generated Javadoc
/**
 * The Interface CredentialStoreService.
 * 
 * @author Sowmya
 */
@Service
public interface CredentialStoreService {

	/**
	 * Creates the credential issuance.
	 *
	 * @param credentialServiceRequestDto the credential service request dto
	 * @return the string
	 */
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto);

	/**
	 * Gets the credential types.
	 *
	 * @return the credential types
	 */
	public CredentialTypeResponse getCredentialTypes();
}
