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
 *  	AITIA
 *
 *******************************************************************************/
package ai.aitia.arrowhead.translationpoc.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.dto.MqttResponseTemplate;

@Service
public class GeneralMqttCallback implements MqttCallback {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ObjectMapper mapper;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void messageArrived(final String topic, final MqttMessage message) throws Exception {
		final MqttResponseTemplate response = mapper.readValue(message.getPayload(), MqttResponseTemplate.class);
		System.out.println("Trace id: " + response.traceId() + ", response status: " + response.status());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void deliveryComplete(final IMqttDeliveryToken token) {
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void connectionLost(final Throwable cause) {
	}
}