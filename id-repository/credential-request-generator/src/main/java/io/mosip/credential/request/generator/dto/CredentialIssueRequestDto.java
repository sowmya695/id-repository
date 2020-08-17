package io.mosip.credential.request.generator.dto;

import java.util.List;

import lombok.Data;

@Data
public class CredentialIssueRequestDto {


	
	private String id;
	private String credentialType;
	private String issuer;
    private String  recepiant;
	private String user;
	private boolean encrypt;
	private String encryptionKey;
    private List<String> sharableAttributes;
	
}
