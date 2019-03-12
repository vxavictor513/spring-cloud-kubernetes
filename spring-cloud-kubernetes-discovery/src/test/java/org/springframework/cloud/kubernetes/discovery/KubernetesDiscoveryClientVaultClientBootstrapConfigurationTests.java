/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.discovery;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.vault.config.DiscoveryClientVaultBootstrapConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.VaultEndpointProvider;

import java.util.Collections;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author Wai Loon Theng
 */
public class KubernetesDiscoveryClientVaultClientBootstrapConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			if (this.context.getParent() != null) {
				((AnnotationConfigApplicationContext) this.context.getParent()).close();
			}
			this.context.close();
		}
	}

	@Test
	public void onWhenRequested() {
		setup("server.port=7000", "spring.cloud.vault.discovery.enabled=true",
			"spring.cloud.kubernetes.discovery.enabled=true",
			"spring.cloud.kubernetes.enabled=true", "spring.application.name=test",
			"spring.cloud.vault.discovery.service-id=vault");
		assertNotNull(this.context.getParent());
		assertEquals(1, this.context.getParent()
			.getBeanNamesForType(DiscoveryClient.class).length);
		DiscoveryClient client = this.context.getParent().getBean(DiscoveryClient.class);
		verify(client, atLeast(1)).getInstances("vault");
		VaultEndpointProvider locator = this.context
			.getBean(VaultEndpointProvider.class);
		assertEquals("https://fake:8200", locator.getVaultEndpoint().toString());
	}

	private void setup(String... env) {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(env).applyTo(parent);
		parent.register(UtilAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, EnvironmentKnobbler.class,
			KubernetesDiscoveryClientConfigClientBootstrapConfiguration.class,
			DiscoveryClientVaultBootstrapConfiguration.class);
		parent.refresh();
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
			KubernetesAutoConfiguration.class,
			KubernetesDiscoveryClientAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	protected static class EnvironmentKnobbler {

		@Bean
		public KubernetesDiscoveryClient kubernetesDiscoveryClient() {
			KubernetesDiscoveryClient client = mock(KubernetesDiscoveryClient.class);
			ServiceInstance instance = new DefaultServiceInstance("vault1",
				"vault", "fake", 8200, false);
			given(client.getInstances("vault"))
				.willReturn(Collections.singletonList(instance));
			return client;
		}

	}

}
