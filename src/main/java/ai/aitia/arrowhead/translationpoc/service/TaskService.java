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
package ai.aitia.arrowhead.translationpoc.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import javax.naming.ConfigurationException;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.translationpoc.AiTranslationPocConsumerMain;
import ai.aitia.arrowhead.translationpoc.mqtt.GeneralMqttClient;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class TaskService {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private GeneralMqttClient mqttClient;

	@Value("${input.folder.path}")
	private String inputFolderPath;

	private boolean initialized = false;

	private OrchestrationResponseDTO orchResponse = null;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void initialize() throws ConfigurationException {
		if (initialized) {
			return;
		}

		if (Utilities.isEmpty(inputFolderPath)) {
			throw new ConfigurationException("Input folder path is not specified");
		}

		if (!Path.of(inputFolderPath).toFile().exists()) {
			throw new ConfigurationException("Input folder path is not exist");
		}

		// get the orchestration service from Service Registry
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION)
				.providerName(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION)
				.interfaceTemplateName(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME)
				.build();
		final ServiceInstanceListResponseDTO response = arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		if (response == null || Utilities.isEmpty(response.entries())) {
			throw new ConfigurationException("DynamicServiceOrchestration system is not found");
		}

		initialized = true;
	}

	//-------------------------------------------------------------------------------------------------
	public void perform() throws IOException {
		if (!initialized) {
			throw new InternalServerError("Service is not initialized");
		}

		try (Stream<Path> stream = Files.list(Path.of(inputFolderPath))) {
			stream
					.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".xml"))
					.forEach(path -> {
						try {
							perform(path);
						} catch (final IOException ex) {
							System.out.println(ex.getMessage());
							ex.printStackTrace();
						}
					});

		}
	}

	//=================================================================================================
	// assistant method

	//-------------------------------------------------------------------------------------------------
	private boolean perform(final Path xmlFilePath) throws IOException {
		if (orchResponse == null) {
			orchestrateService();
		}

		final String xmlPayload = Files
				.readString(xmlFilePath, StandardCharsets.UTF_8);

		final OrchestrationResultDTO orchResult = orchResponse.results().get(0);
		final String topic = getTargetTopic(orchResult);
		final String bridgeToken = orchResult
				.authorizationTokens()
				.get(ServiceInterfacePolicy.TRANSLATION_BRIDGE_TOKEN_AUTH.name())
				.get("save-ipc2581")
				.token();

		final MqttRequestTemplate template = new MqttRequestTemplate(
				xmlFilePath.getFileName().toString(),
				bridgeToken,
				AiTranslationPocConsumerMain.MQTT_RESPONSE_TOPIC,
				MqttQoS.AT_MOST_ONCE.value(),
				Map.of(),
				xmlPayload);

		try {
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			mqttClient.publish(topic, msg);
			System.out.println("Consume message is sent with the content of file " + xmlFilePath.getFileName().toString());

			return true;
		} catch (final Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();

			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void orchestrateService() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("dataModels.save-ipc2581.input", "ec1");

		final OrchestrationRequestDTO orchPayload = new OrchestrationRequestDTO.Builder()
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder()
						.serviceDefinition("saveData")
						.operation("save-ipc2581")
						.interfaceTemplateName(Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME)
						.interfacePropertyRequirement(req)
						.build())
				.orchestrationFlag(OrchestrationFlag.MATCHMAKING.name(), true)
				.orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.build();

		System.out.println("Orchestrating for 'saveData'");
		orchResponse = arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_ORCHESTRATION,
				Constants.SERVICE_OP_ORCHESTRATION_PULL,
				Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION,
				OrchestrationResponseDTO.class,
				orchPayload);

		if (orchResponse == null || Utilities.isEmpty(orchResponse.results())) {
			throw new InternalServerError("Orchestration does not found any service provider for 'saveData'");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private String getTargetTopic(final OrchestrationResultDTO orchestrationResultDTO) {
		final ServiceInstanceInterfaceResponseDTO intf = orchestrationResultDTO.interfaces().get(0);

		return intf.properties().get("baseTopic") + "save-ipc2581";
	}
}