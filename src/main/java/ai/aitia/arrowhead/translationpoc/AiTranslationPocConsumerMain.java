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

import java.util.UUID;

import javax.naming.ConfigurationException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.translationpoc.mqtt.GeneralMqttClient;
import ai.aitia.arrowhead.translationpoc.service.TaskService;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.security.DefaultSecurityConfig;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import jakarta.annotation.PreDestroy;

@SpringBootApplication(exclude = {
	    DataSourceAutoConfiguration.class, 
	    DataSourceTransactionManagerAutoConfiguration.class, 
	    HibernateJpaAutoConfiguration.class
})
@ComponentScan(
	basePackages = { Constants.BASE_PACKAGE, Constants.COMMON_BASE_PACKAGE }, 
	excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { DefaultSecurityConfig.class }) })
public class AiTranslationPocConsumerMain implements CommandLineRunner {

	//=================================================================================================
	// members

	public static final String MQTT_RESPONSE_TOPIC = "consumer/translation/ai" + UUID.randomUUID().toString();

	@Autowired
	private AiTranslationPocConsumerSystemInfo systemInfo;

	@Autowired
	private ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private GeneralMqttClient mqttClient;
	
	@Autowired
	private TaskService taskService;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		final ConfigurableApplicationContext context = SpringApplication.run(AiTranslationPocConsumerMain.class, args);
		context.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run(final String... args) throws Exception {
		initialize();
		
		taskService.initialize();
		taskService.perform();
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void destroy() {
		try {
			if (mqttClient != null) {
				mqttClient.unsubscribe(MQTT_RESPONSE_TOPIC);
				mqttClient.destroy();
			}
		} catch (final MqttException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void initialize() throws ConfigurationException {
		// register system
		final SystemModel model = systemInfo.getSystemModel();
		final SystemRegisterRequestDTO payload = new SystemRegisterRequestDTO(model.metadata(), model.version(), model.addresses(), model.deviceName());
		arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SYSTEM_DISCOVERY, Constants.SERVICE_OP_REGISTER, Constants.SYS_NAME_SERVICE_REGISTRY, SystemResponseDTO.class, payload);

		// connect to MQTT
		try {
			mqttClient.initialize();
			mqttClient.subscribe(MQTT_RESPONSE_TOPIC);
		} catch (final MqttException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			throw new ConfigurationException("Can't access the MQTT broker: " + ex.getMessage());
		}
	}
}