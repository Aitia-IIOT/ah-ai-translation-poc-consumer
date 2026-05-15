/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package ai.aitia.arrowhead.translationpoc;

import java.util.List;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component
public class AiTranslationPocConsumerSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			systemModel = new SystemModel.Builder()
					.address(getAddress())
					.version("1.0.0")
					.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	public String getIdentityToken() {
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public AuthenticationPolicy getAuthenticationPolicy() {
		return AuthenticationPolicy.DECLARED;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isSslEnabled() {
		return false;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit() {
		if (Utilities.isEmpty(getMqttBrokerAddress())) {
			throw new InvalidParameterException("MQTT Broker address is not defined");
		}

		if (getMqttBrokerPort() == null) {
			throw new InvalidParameterException("MQTT Broker port is not defined");
		}
	}
}