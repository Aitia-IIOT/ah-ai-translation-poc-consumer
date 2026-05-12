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

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.translationpoc.AiTranslationPocConsumerSystemInfo;
import eu.arrowhead.common.Utilities;

@Component
public class GeneralMqttClient {

	//=================================================================================================
	// members

	private static final String TCP_PREFIX = Constants.TCP + "://";

	private MqttClient client = null;

	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
	private AiTranslationPocConsumerSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void initialize() throws MqttException {
		final MqttClient client = createAndConnect();
		final GeneralMqttCallback callback = appContext.getBean(GeneralMqttCallback.class);
		client.setCallback(callback);
		this.client = client;
	}

	//-------------------------------------------------------------------------------------------------
	public void destroy() throws MqttException {
		client.disconnect();
		client.close();
		client = null;
	}

	//-------------------------------------------------------------------------------------------------
	public void subscribe(final String topic) throws MqttException {
		if (client != null
				&& !Utilities.isEmpty(topic)) {
			client.subscribe(topic.trim());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void unsubscribe(final String topic) throws MqttException {
		if (client != null
				&& !Utilities.isEmpty(topic)) {
			client.unsubscribe(topic.trim());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String getServerURI() {
		return client != null ? client.getServerURI() : "";
	}

	//-------------------------------------------------------------------------------------------------
	public void publish(final String topic, final MqttMessage msg) throws MqttPersistenceException, MqttException {
		if (client != null
				&& !Utilities.isEmpty(topic)
				&& msg != null) {
			client.publish(topic.trim(), msg);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private MqttClient createAndConnect() throws MqttException {
		final String serverURI = TCP_PREFIX
				+ sysInfo.getMqttBrokerAddress()
				+ ":"
				+ sysInfo.getMqttBrokerPort();

		final MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setUserName(sysInfo.getSystemName());
		if (!Utilities.isEmpty(sysInfo.getMqttClientPassword())) {
			options.setPassword(sysInfo.getMqttClientPassword().toCharArray());
		}

		final MqttClient client = new MqttClient(serverURI, sysInfo.getSystemName() + "-" + UUID.randomUUID().toString());
		client.connect(options);

		return client;
	}
}