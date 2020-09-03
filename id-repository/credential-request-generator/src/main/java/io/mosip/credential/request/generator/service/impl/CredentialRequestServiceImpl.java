package io.mosip.credential.request.generator.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.batch.config.CredentialItemProcessor;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.dto.Event;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialrRequestGeneratorException;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.util.Utilities;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;

/**
 * 
 * @author Sowmya
 *
 * The Class CredentialRequestServiceImpl.
 */
@Component
public class CredentialRequestServiceImpl implements CredentialRequestService {
	
	/** The credential repositary. */
	@Autowired
	CredentialRepositary<CredentialEntity, String> credentialRepositary;
	
	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.request.datetime.pattern";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_ID. */
	private static final String CREDENTIAL_REQUEST_SERVICE_ID = "mosip.credential.request.service.id";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_VERSION. */
	private static final String CREDENTIAL_REQUEST_SERVICE_VERSION = "mosip.credential.request.service.version";

	@Autowired
	private ObjectMapper mapper;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemProcessor.class);
	
	@Autowired
	private Utilities utilities;
	
	/** The Constant BIOMETRICS. */
	private static final String CREATE_CREDENTIAL = "createCredentialIssuance";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String CREDENTIAL_SERVICE = "CredentialRequestServiceImpl";
	
	@Autowired
	private AuditHelper auditHelper;
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credential.request.generator.service.CredentialRequestService#
	 * createCredentialIssuance(io.mosip.credential.request.generator.dto.
	 * CredentialIssueRequestDto)
	 */
	@Override
	public ResponseWrapper<CredentialIssueResponse> createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"started creating credential");
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();

		CredentialIssueResponse credentialIssueResponse = null;
		try{
			String requestId = utilities.generateId();
		

	    CredentialEntity credential=new CredentialEntity();
		credential.setRequestId(requestId);
			credential.setRequest(mapper.writeValueAsString(credentialIssueRequestDto));
			credential.setStatusCode(CredentialStatusCode.NEW.name());
		credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setCreatedBy(USER);

		credentialRepositary.save(credential);
		credentialIssueResponse = new CredentialIssueResponse();
		credentialIssueResponse.setRequestId(requestId);
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL,
				"ended creating credential");
	    }catch(DataAccessLayerException e) {
	    	auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID, e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SERVICE, CREATE_CREDENTIAL, ExceptionUtils.getStackTrace(e));
		} finally {
			credentialIssueResponseWrapper.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			credentialIssueResponseWrapper
					.setResponsetime(localdatetime);
			credentialIssueResponseWrapper.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CREATING_CREDENTIAL_REQUEST, credentialIssueRequestDto.getId(), IdType.ID,"create credential request requested");
		}
		return credentialIssueResponseWrapper;
	}

	
	@Override
	public ResponseWrapper<CredentialIssueResponse> cancelCredentialRequest(String requestId) {
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = new ResponseWrapper<CredentialIssueResponse>();

		CredentialIssueResponse credentialIssueResponse = null;
		try {
			Optional<CredentialEntity> entity = credentialRepositary.findById(requestId);
			if (entity != null) {
				CredentialEntity credentialEntity = entity.get();
				credentialEntity.setStatusCode(CredentialStatusCode.CANCELLED.name());
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(USER);
				credentialRepositary.update(credentialEntity);
				credentialIssueResponse = new CredentialIssueResponse();
				credentialIssueResponse.setRequestId(requestId);
			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);
			}
		} catch (DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);

		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
		} finally {
			credentialIssueResponseWrapper.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			credentialIssueResponseWrapper
					.setResponsetime(localdatetime);
			credentialIssueResponseWrapper.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueResponseWrapper.setResponse(credentialIssueResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.CANCEL_CREDENTIAL_REQUEST, requestId, IdType.ID,"Cancel the request");
		}
		return credentialIssueResponseWrapper;
	}


	@Override
	public ResponseWrapper<CredentialIssueStatusResponse> getCredentialRequestStatus(String requestId) {
	
		List<ServiceError> errorList = new ArrayList<>();
		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueStatusResponseWrapper = new ResponseWrapper<CredentialIssueStatusResponse>();

		CredentialIssueStatusResponse credentialIssueStatusResponse = new CredentialIssueStatusResponse();
		try {
			Optional<CredentialEntity> entity = credentialRepositary.findById(requestId);
			if (entity != null) {
				CredentialEntity credentialEntity = entity.get();
				credentialIssueStatusResponse.setRequestId(requestId);
				credentialIssueStatusResponse.setStatusCode(credentialEntity.getStatusCode());
			} else {
				ServiceError error = new ServiceError();
				error.setErrorCode(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorCode());
				error.setMessage(CredentialRequestErrorCodes.REQUEST_ID_ERROR.getErrorMessage());
				errorList.add(error);
			
			}
		} catch (DataAccessLayerException e) {
		
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage());
			errorList.add(error);

		} catch (Exception e) {
		
			ServiceError error = new ServiceError();
			error.setErrorCode(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialRequestErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
		} finally {
			credentialIssueStatusResponseWrapper.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			credentialIssueStatusResponseWrapper
					.setResponsetime(localdatetime);
			credentialIssueStatusResponseWrapper.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueStatusResponseWrapper.setErrors(errorList);
			} else {
				credentialIssueStatusResponseWrapper.setResponse(credentialIssueStatusResponse);
			}
			
		}
		return credentialIssueStatusResponseWrapper;
	}


	@Override
	public void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialrRequestGeneratorException {
		String requestId=null;
		try {
			Event event=credentialStatusEvent.getEvent();
			requestId=credentialStatusEvent.getEvent().getRequestId();
			Optional<CredentialEntity> entity = credentialRepositary.findById(requestId);
			if (entity != null) {
				CredentialEntity credentialEntity = entity.get();
				credentialEntity.setStatusCode(event.getStatus());
				if(!StringUtils.isEmpty(event.getUrl())) {
					credentialEntity.setDataShareUrl(event.getUrl());
				}
				credentialEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				credentialEntity.setUpdatedBy(USER);
				credentialRepositary.update(credentialEntity);
				
			} else {
				throw new CredentialrRequestGeneratorException();
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,"update the request");
		}catch (DataAccessLayerException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,e);
			
			throw new CredentialrRequestGeneratorException();

		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_REQUEST_GENERATOR, AuditEvents.UPDATE_CREDENTIAL_REQUEST, requestId, IdType.ID,e);

			throw new CredentialrRequestGeneratorException();		
			}
		
	}
	
}
